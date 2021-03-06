//
//Copyright (c) 1998,2007 Michael Toth
//Spiralcraft Inc., All Rights Reserved
//
//This package is part of the Spiralcraft project and is licensed under
//a multiple-license framework.
//
//You may not use this file except in compliance with the terms found in the
//SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
//at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
//Unless otherwise agreed to in writing, this software is distributed on an
//"AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.servlet.webui;

import spiralcraft.text.markup.MarkupException;
import spiralcraft.util.thread.BlockTimer;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.servlet.kit.ContextAdapter;
import spiralcraft.servlet.webui.textgen.UIResourceUnit;
import spiralcraft.servlet.webui.textgen.RootUnit;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.servlet.ServletException;

/**
 * <p>Provides references to the bound user interface component associated with
 *    a specific relative path.
 * </p>
 * 
 * <p>Components are parsed and bound as needed.
 * </p>
 */
public class UICache
{
  private static final ClassLog log=ClassLog.getInstance(UICache.class);
  
  private HashMap<String,UIResourceUnit> textgenCache
    =new HashMap<String,UIResourceUnit>();
  
  private final HashMap<String,ComponentReference> componentMap
    =new HashMap<String,ComponentReference>();
  
  private final Focus<?> focus;

  private int resourceCheckFrequencyMs=5000;

  private boolean showExceptions=false;
  private String exceptionResource
    ="class:/spiralcraft/servlet/webui/resources/loadTimeErrorReport.tgl";
  
  /**
   * Create a new UI Cache with components bound to the specified Focus
   */
  public UICache(Focus<?> parentFocus,ContextAdapter context)
  {
    this.focus=parentFocus;
    this.showExceptions
      ="true".equals
        (context.getContext().getInitParameter
          ("spiralcraft.servlet.showExceptions")
        );
    
    String contextExceptionResource
      =context.getContext().getInitParameter
          ("spiralcraft.servlet.webui.exceptionResource");
    if (contextExceptionResource!=null)
    { this.exceptionResource=contextExceptionResource;
    }
        
  }  
  
  /**
   * <p>Get or create a bound UI RootComponent defined by a source code 
   *   Resource and uniquely identified by a ServletContext relative path 
   *   String.
   * </p>
   *   
   * 
   * @param resource The source code file for the UI component
   * @param instancePath The path relative to the ServletContext
   *   that uniquely identifies the UI component
   *   
   * @return The RootComponent associated with the path
   * @throws MarkupException
   * @throws IOException
   * @throws ServletException
   */
  public synchronized RootComponent getUI
    (Resource resource,String instancePath)
    throws ContextualException,IOException,ServletException
  {
    UIResourceUnit resourceUnit
      =resolveResourceUnit(resource,instancePath);
    if (resourceUnit!=null)
    { 
      try
      {
        RootUnit unit=resourceUnit.getUnit();
      
        if (unit!=null)
        { return getComponent(instancePath,unit);
        }
        else
        { 
          // XXX Return an 'exception handler' component
          if (resourceUnit.getException()!=null)
          { 
            if (showExceptions)
            {
              return makeExceptionComponent
                (resource,instancePath,resourceUnit,resourceUnit.getException());
            }
            else
            { 
              throw new ServletException
                ("Error instantiating component for path ["+instancePath
                  +"] from resource ["+resource.getURI()+"]"
                ,resourceUnit.getException()
                );
            }
          }
          return null;
        }
      }
      catch (ContextualException x)
      { 
        if (showExceptions)
        {
          return makeExceptionComponent(resource,instancePath,null,x);
        }
        else
        {
          throw new ServletException
            ("Error compiling path ["+instancePath
              +"] resource ["+resource.getURI()+"]"
            ,x
            );
        }
      }
      
    }
    else
    { return null;
    }
  }
  
  private RootComponent makeExceptionComponent
    (Resource resource,String instancePath,UIResourceUnit resourceUnit,Exception exception)
    throws ServletException,ContextualException,IOException
  {
    ExceptionInfo exceptionInfo
      =new ExceptionInfo(resource,instancePath,resourceUnit,exception);
        
    if (exceptionResource!=null)
    {
      Resource errorResource=Resolver.getInstance().resolve(exceptionResource);
      if (errorResource!=null && errorResource.exists())
      {
        UIResourceUnit errorUnit
          =new UIResourceUnit
            (errorResource);
        RootUnit errorRoot=errorUnit.getUnit();
        if (errorRoot!=null)
        {
          return errorRoot.bindRoot
              (focus.chain(new SimpleChannel<>(exceptionInfo,true))
              );
        }
        else
        { 
          log.log
            (Level.WARNING
            ,"Could not compile error handler "+exceptionResource
            ,errorUnit.getException()
            );
        }
      }
      else
      { log.warning("Could not find exception resource "+exceptionResource);
      }
    }

    
    ExceptionComponent component
      =new ExceptionComponent
        (exceptionInfo
        );
    try
    { component.bind(null);
    }
    catch (BindException x)
    { throw new ServletException(x);
    }
    return component;
    
  }
  
  /**
   * <p>Find or create the UIResourceUnit that references the compiled textgen 
   *  doclet within the specified context.
   * </p>
   * 
   * @param resource  The path relative to the ServletContext
   *   of the textgen source doclet
   * @param instancePath  A path string corresponding to the contextually
   *   scoped instance of the UIResourceUnit. The instancePath is used to cache
   *   the UIResourceUnit generated from the source code file within a given
   *   context.
   *  
   * @return
   * @throws ServletException
   * @throws IOException
   */
  private UIResourceUnit resolveResourceUnit(Resource resource,String instancePath)
    throws ServletException,IOException
  {
    UIResourceUnit resourceUnit=textgenCache.get(instancePath);
    if (resourceUnit!=null)
    { 
      //log.fine(toString()+": Found "+instancePath+" in cache");
      return resourceUnit;
    }
    
    if (!resource.exists())
    { return null;
    }
    
    // Force check for directory so we can redirect
    InputStream in=resource.getInputStream();
    in.close();
    
    resourceUnit=new UIResourceUnit(resource);
    resourceUnit.setCheckFrequencyMs(resourceCheckFrequencyMs);
    textgenCache.put(instancePath,resourceUnit);
    //log.fine(toString()+": Added "+instancePath+" to cache");
    return resourceUnit;
  }
  
  /**
   * <p>Get or create the component instance that is identified by the specified 
   *   path relative to the ServletContext and is derived from the provided
   *   RootUnit instance.
   * </p>
   * 
   * <p>If the instancePath is not found, or the supplied RootUnit is different
   *   than the previously bound unit, a new RootComponent will be returned.
   * </p>
   * 
   * @param contextRelativePath
   * @return
   */
  private RootComponent 
    getComponent(String instancePath,RootUnit unit)
    throws ContextualException
  {
    ComponentReference ref=componentMap.get(instancePath);
    if (ref!=null && ref.unit==unit)
    { return ref.component;
    }
    ref=new ComponentReference();
    ref.unit=unit;
    BlockTimer timer=new BlockTimer();
    timer.push();
    try
    {
      ref.component=unit.bindRoot(focus);
      log.info("Bound "+unit.getSourceURI()+" in "+timer.elapsedTimeFormatted());
    }
    finally
    { timer.pop();
    }
    ref.component.setInstancePath(instancePath);
    componentMap.put(instancePath,ref);
    return ref.component;
    
  }

  
}

class ComponentReference
{
  public RootComponent component;
  public RootUnit unit;
}

