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

import spiralcraft.textgen.compiler.DocletUnit;

import spiralcraft.servlet.webui.components.UiComponent;

import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;

import spiralcraft.lang.spi.SimpleBinding;


import java.util.HashMap;

import javax.servlet.ServletException;

/**
 * <P>Contains the state of a UI for a specific user over a period of
 * interaction.
 * </P>
 * 
 * <P>The Session class may be extended for enhanced functionality.
 * </P>

 * <P>A Session is stored in the HttpSession.
 * </P>
 */
public class Session
{
  
  private final HashMap<String,ComponentReference> componentMap
    =new HashMap<String,ComponentReference>();
  
  private CompoundFocus<Session> focus;


  public void init(Focus<?> parentFocus)
    throws ServletException
  {
    try
    { 
      focus=new CompoundFocus<Session>();
      focus.setParentFocus(parentFocus);
      focus.setName("session");
      focus.addNamespaceAlias("webui","spiralcraft.servlet.webui");
      focus.setSubject(new SimpleBinding<Session>(this,true));
    }
    catch (BindException x)
    { throw new ServletException("Error binding Session "+x,x);
    }
  }
  
  /**
   * Get the component that is identified by the specified path
   * 
   * @param relativePath
   * @return
   */
  public synchronized UiComponent 
    getComponent(String relativePath,DocletUnit unit)
    throws MarkupException
  {
    ComponentReference ref=componentMap.get(relativePath);
    if (ref!=null && ref.unit==unit)
    { return ref.component;
    }
    ref=new ComponentReference();
    ref.unit=unit;
    ref.component=new UiComponent(unit.bind(focus));
    componentMap.put(relativePath,ref);
    return ref.component;
    
  }

  
}

class ComponentReference
{
  public UiComponent component;
  public DocletUnit unit;
}
