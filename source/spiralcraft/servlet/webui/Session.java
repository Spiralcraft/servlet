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
 *  to a servlet) for a specific user over a period of interaction.
 * </p>
 * 
 * <p>The Session class may be extended for enhanced functionality.
 * </p>

 * <p>A Session is stored in the HttpSession.
 * </p>
 * 
 * <p>A Session contains a reference to the ResourceSession for each
 *   active WebUI resource in the ServletContext, mapped by the path
 *   of the resource relative to the ServletContext.
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
   * Get the ResourceSession associated with the UiComponent resource
   * 
   * @param component
   */
  public synchronized ResourceSession
    getResourceSession(UIComponent component)
  {
    StateReference ref=stateMap.get(component.getContextRelativePath());
    if (ref!=null && ref.component==component)
    { return ref.localSession;
    }
    else if (ref==null)
    {
      ref=new StateReference();
      stateMap.put(component.getContextRelativePath(),ref);
    }
    else
    {
      ref.component=component;
      ref.localSession=null;
    }
    return ref.localSession;
    
  }

  /**
   * Set the ResourceSession associated with the UiComponent resource
   * 
   * @param component
   */
  public synchronized void
    setResourceSession(UIComponent component,ResourceSession localSession)
  {
    StateReference ref=stateMap.get(component.getContextRelativePath());
    if (ref==null)
    { 
      ref=new StateReference();
      stateMap.put(component.getContextRelativePath(), ref);
    }
    ref.component=component;
    ref.localSession=localSession;
  }
  
}

class StateReference
{
  public ResourceSession localSession;
  public UIComponent component;
}
