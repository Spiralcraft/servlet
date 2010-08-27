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
import spiralcraft.servlet.webui.textgen.RootUnit;


import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;


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
  
  private final Focus<?> focus;

  private int resourceCheckFrequencyMs=5000;

  /**
   * Create a new UI Cache with components bound to the specified Focus
   */
  public UICache(Focus<?> parentFocus)
  { this.focus=parentFocus;
  }  
  
  /**
   * 
   * @param contextRelativePath The path relative to the ServletContext
   *   that uniquely identifies the UI component
   *   
   * @return The RootComponent associated with the path
   * @throws MarkupException
   * @throws IOException
   * @throws ServletException
   */
  public synchronized RootComponent getUI
    (Resource resource,String instancePath)
    throws MarkupException,IOException,ServletException
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
            ExceptionComponent component
              =new ExceptionComponent(resourceUnit.getException());
            try
            { component.bind(null,null);
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
        { component.bind(null,null);
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
  private UIResourceUnit resolveResourceUnit(Resource resource,String instancePath)
    throws ServletException,IOException
  {
    UIResourceUnit resourceUnit=textgenCache.get(instancePath);
    
    if (resourceUnit!=null)
    { return resourceUnit;
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
    return resourceUnit;
  }
  
  /**
   * Get the component instance that is identified by the specified path
   *   relative to the ServletContext
   * 
   * @param contextRelativePath
   * @return
   */
  private RootComponent 
    getComponent(String instancePath,RootUnit unit)
    throws MarkupException
  {
    ComponentReference ref=componentMap.get(instancePath);
    if (ref!=null && ref.unit==unit)
    { return ref.component;
    }
    ref=new ComponentReference();
    ref.unit=unit;
    ref.component=unit.bindRoot(focus);
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
