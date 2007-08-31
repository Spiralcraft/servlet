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


import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;

import spiralcraft.lang.spi.SimpleBinding;


import java.io.IOException;
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
  
  private final CompoundFocus<UIServlet> focus;
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
      focus=new CompoundFocus<UIServlet>();
      focus.setParentFocus(parentFocus);
      focus.setName("servlet");
      focus.addNamespaceAlias("webui","spiralcraft.servlet.webui");
      focus.setSubject(new SimpleBinding<UIServlet>(servlet,true));
    }
    catch (BindException x)
    { throw new ServletException("Error binding Session "+x,x);
    }
  }  
  
  public synchronized UIComponent getUI(String relativePath)
    throws MarkupException,IOException,ServletException
  {
    UIResourceUnit resourceUnit=resolveResourceUnit(relativePath);
    if (resourceUnit!=null)
    { 
      UIUnit unit=resourceUnit.getUnit();
      if (unit!=null)
      { return getComponent(relativePath,unit);
      }
      else
      { 
        // XXX Return an 'exception handler' component
        if (resourceUnit.getException()!=null)
        { 
          throw new ServletException
            ("Error loading WebUI resource "+resourceUnit.getException()
            ,resourceUnit.getException()
            );
        }
        return null;
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
   * @param relativePath
   * @return
   * @throws ServletException
   * @throws IOException
   */
  private UIResourceUnit resolveResourceUnit(String relativePath)
    throws ServletException,IOException
  {
    UIResourceUnit resourceUnit=textgenCache.get(relativePath);
    
    if (resourceUnit!=null)
    { return resourceUnit;
    }
    
    Resource resource=servlet.getResource(relativePath);
    if (!resource.exists())
    { return null;
    }
    
    resourceUnit=new UIResourceUnit(resource);
    resourceUnit.setCheckFrequencyMs(resourceCheckFrequencyMs);
    textgenCache.put(relativePath,resourceUnit);
    return resourceUnit;
  }
  
  /**
   * Get the component that is identified by the specified path
   * 
   * @param relativePath
   * @return
   */
  private UIComponent 
    getComponent(String relativePath,UIUnit unit)
    throws MarkupException
  {
    ComponentReference ref=componentMap.get(relativePath);
    if (ref!=null && ref.unit==unit)
    { return ref.component;
    }
    ref=new ComponentReference();
    ref.unit=unit;
    ref.component=unit.bind(focus);
    ref.component.setRelativePath(relativePath);
    componentMap.put(relativePath,ref);
    return ref.component;
    
  }

  
}

class ComponentReference
{
  public UIComponent component;
  public UIUnit unit;
}
