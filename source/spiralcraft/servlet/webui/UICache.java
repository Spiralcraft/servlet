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
import spiralcraft.vfs.Resource;

import spiralcraft.servlet.webui.textgen.UIResourceUnit;
import spiralcraft.servlet.webui.textgen.UIUnit;


import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;

import spiralcraft.lang.spi.SimpleChannel;


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
  
  private HashMap<String,UIResourceUnit> textgenCache
    =new HashMap<String,UIResourceUnit>();
  
  private final HashMap<String,ComponentReference> componentMap
    =new HashMap<String,ComponentReference>();
  
  private final SimpleFocus<UIServlet> focus;
  private final UIServlet servlet;

  private int resourceCheckFrequencyMs=5000;

  /**
   * Create a new UI Cache with components bound to the specified Focus
   */
  public UICache(UIServlet servlet,Focus<?> parentFocus)
    throws ServletException
  { 
    this.servlet=servlet;
    try
    { 
      focus=new SimpleFocus<UIServlet>();
      focus.setParentFocus(parentFocus);
      focus.setSubject(new SimpleChannel<UIServlet>(servlet,true));
    }
    catch (BindException x)
    { throw new ServletException("Error binding Session "+x,x);
    }
  }  
  
  /**
   * 
   * @param contextRelativePath The path relative to the ServletContext
   *   that uniquely identifies the UI component
   *   
   * @return The UIComponent associated with the path
   * @throws MarkupException
   * @throws IOException
   * @throws ServletException
   */
  public synchronized UIComponent getUI(String contextRelativePath)
    throws MarkupException,IOException,ServletException
  {
    UIResourceUnit resourceUnit=resolveResourceUnit(contextRelativePath);
    if (resourceUnit!=null)
    { 
      try
      {
        UIUnit unit=resourceUnit.getUnit();
      
        if (unit!=null)
        { return getComponent(contextRelativePath,unit);
        }
        else
        { 
          // XXX Return an 'exception handler' component
          if (resourceUnit.getException()!=null)
          { 
            ExceptionComponent component
              =new ExceptionComponent(resourceUnit.getException());
            try
            { component.bind(null);
            }
            catch (BindException x)
            { throw new ServletException(x);
            }
            return component;
            
          }
          return null;
        }
      }
      catch (MarkupException x)
      { 
        ExceptionComponent component
          =new ExceptionComponent(x);
        try
        { component.bind(null);
        }
        catch (BindException y)
        { throw new ServletException(y);
        }
        return component;
      }
      
    }
    else
    { return null;
    }
  }
  
  /**
   * Find or create the ResourceUnit that references the compiled textgen 
   *  doclet.
   * 
   * @param contextRelativePath The path relative to the ServletContext
   *   of the textgen source doclet
   * @return
   * @throws ServletException
   * @throws IOException
   */
  private UIResourceUnit resolveResourceUnit(String contextRelativePath)
    throws ServletException,IOException
  {
    UIResourceUnit resourceUnit=textgenCache.get(contextRelativePath);
    
    if (resourceUnit!=null)
    { return resourceUnit;
    }
    
    Resource resource=servlet.getResource(contextRelativePath);
    if (!resource.exists())
    { return null;
    }
    
    // Force check for directory so we can redirect
    InputStream in=resource.getInputStream();
    in.close();
    
    resourceUnit=new UIResourceUnit(resource);
    resourceUnit.setCheckFrequencyMs(resourceCheckFrequencyMs);
    textgenCache.put(contextRelativePath,resourceUnit);
    return resourceUnit;
  }
  
  /**
   * Get the component that is identified by the specified path
   *   relative to the ServletContext
   * 
   * @param contextRelativePath
   * @return
   */
  private UIComponent 
    getComponent(String contextRelativePath,UIUnit unit)
    throws MarkupException
  {
    ComponentReference ref=componentMap.get(contextRelativePath);
    if (ref!=null && ref.unit==unit)
    { return ref.component;
    }
    ref=new ComponentReference();
    ref.unit=unit;
    ref.component=unit.bind(focus);
    ref.component.setContextRelativePath(contextRelativePath);
    componentMap.put(contextRelativePath,ref);
    return ref.component;
    
  }

  
}

class ComponentReference
{
  public UIComponent component;
  public UIUnit unit;
}
