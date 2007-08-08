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

import spiralcraft.util.Path;

import java.io.IOException;

/**
 * Basic unit of web user interface composition. 
 * 
 * Components form a tree under a root component. The root component
 *   is normally addressable via a URI path. 
 * 
 * @author mike
 *
 */
public abstract class Component
{

  private Component parent;
  private Path path;
  private String localName;
  
  public void init(Component parent)
  { 
    this.parent=parent;
    if (parent!=null)
    { this.path=parent.getPath().append(localName);
    }
  }
  
  /**
   * Render the component
   * 
   * @param context
   */
  public abstract void render(ServiceContext context)
    throws IOException;
  
  public Path getPath()
  { return path;
  }
  
  public Component getParent()
  { return parent;
  }
  
  public void destroy()
  {
  }

}
