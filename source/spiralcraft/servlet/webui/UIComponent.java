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

import spiralcraft.time.Clock;

import spiralcraft.lang.Focus;


/**
 * <p>Represents the root of a WebUI component tree
 * </p>
 * 
 * <p>The UiComponent is the Component which is addressed directly via the HTTP
 *   client and provides the UI with some control over the HTTP interaction.
 * </p>
 * 
 * 
 * 
 * @author mike
 *
 */
public class UIComponent
  extends Component
{
  
  private final Focus<?> focus;
  private String contextRelativePath;
  
  public UIComponent(Focus<?> focus)
  { this.focus=focus;
  }
  
  /**
   * 
   * @param contextRelativePath The path of this UIComponent relative
   *   to the containing ServletContext
   */
  void setContextRelativePath(String contextRelativePath)
  { this.contextRelativePath=contextRelativePath;
  }
  
  /**
   * 
   * @return The path of this UIComponent relative
   *   to the containing ServletContext
   */
  public String getContextRelativePath()
  { return contextRelativePath;
  }
  
  @Override
  public Focus<?> getFocus()
  { return focus;
  }
  
  /**
   * @return The mime type of the content returned by this
   *   Component
   */
  public String getContentType()
  { return "text/html";
  }
  
  /**
   * Returns the time this UI was last modified. By default,
   *   returns the current time. Override to return a specific
   *   time for resources that are not regenerated for each
   *   request.
   * 
   * @return The last modified time in milliseconds since 1970.
   */
  public long getLastModified()
  { return Clock.instance().approxTimeMillis();
  }


}

