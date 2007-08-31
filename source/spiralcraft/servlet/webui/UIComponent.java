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
 * The root of a WebUI component tree.
 * 
 * The UiComponent is the Component which is addressed directly via the HTTP
 *   client and provides the UI with some control over the HTTP interaction.
 * 
 * @author mike
 *
 */
public class UIComponent
  extends Component<Void>
{
  
  private final Focus<?> focus;
  private String relativePath;
  
  public UIComponent(Focus<?> focus)
  { this.focus=focus;
  }
  
  public void setRelativePath(String relativePath)
  { this.relativePath=relativePath;
  }
  
  public String getRelativePath()
  { return relativePath;
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

