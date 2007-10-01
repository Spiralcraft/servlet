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

import spiralcraft.lang.Focus;

import java.util.HashMap;

import javax.servlet.ServletException;

/**
 * <p>Contains the state of a UI (a set of resources in a directory mapped
 *  to a servlet) for a specific user over a period of
 * interaction.
 * </p>
 * 
 * <p>The Session class may be extended for enhanced functionality.
 * </p>

 * <p>A Session is stored in the HttpSession.
 * </p>
 */
public class Session
{
  
  private final HashMap<String,StateReference> stateMap
    =new HashMap<String,StateReference>();
  



  public void init(Focus<?> parentFocus)
    throws ServletException
  {
  }
  
  /**
   * Get the ElementState associated with the UiComponent for this session
   * 
   * @param component
   */
  public synchronized LocalSession
    getLocalSession(UIComponent component)
  {
    StateReference ref=stateMap.get(component.getRelativePath());
    if (ref!=null && ref.component==component)
    { return ref.localSession;
    }
    else if (ref==null)
    {
      ref=new StateReference();
      stateMap.put(component.getRelativePath(),ref);
    }
    else
    {
      ref.component=component;
      ref.localSession=null;
    }
    return ref.localSession;
    
  }

  /**
   * Set the ElementState associated with the UiComponent for this session
   * 
   * @param component
   */
  public synchronized void
    setLocalSession(UIComponent component,LocalSession localSession)
  {
    StateReference ref=stateMap.get(component.getRelativePath());
    if (ref==null)
    { 
      ref=new StateReference();
      stateMap.put(component.getRelativePath(), ref);
    }
    ref.component=component;
    ref.localSession=localSession;
  }
  
}

class StateReference
{
  public LocalSession localSession;
  public UIComponent component;
}
