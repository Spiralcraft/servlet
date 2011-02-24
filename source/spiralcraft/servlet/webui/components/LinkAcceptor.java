//
//Copyright (c) 2011 Michael Toth
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
package spiralcraft.servlet.webui.components;


import java.io.IOException;

import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.EventContext;

/**
 * Coordinates behavior triggered by responsive links
 * 
 * @author mike
 *
 * @param <T>
 */
public class LinkAcceptor<T>
  extends Acceptor<T>
{
  //private static final ClassLog log=ClassLog.getInstance(Controller.class);
  
  @Override
  protected boolean wasActioned(ServiceContext context)
  { return !context.getOutOfSync();
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    Action action=createAction(context,true);
    ((ServiceContext) context).registerAction(action);
    super.render(context);
  }
  
  /**
   * Called by contained links to trigger the post-processing action
   *   after a link is clicked.
   */
  public void linkActioned()
  { 
    ServiceContext.get().queueAction(pathToActionName(getState().getPath()));
  }
  
}
    

