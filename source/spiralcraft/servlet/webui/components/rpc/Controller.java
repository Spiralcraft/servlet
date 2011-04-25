//
//Copyright (c) 2008,2008 Michael Toth
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
package spiralcraft.servlet.webui.components.rpc;


//import spiralcraft.log.ClassLog;


import spiralcraft.servlet.webui.RequestMessage;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.Acceptor;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.MessageHandlerChain;

import spiralcraft.textgen.kit.AbstractMessageHandler;

import spiralcraft.app.Message;

public class Controller<T>
  extends Acceptor<T>
{
  //private static final ClassLog log=ClassLog.getInstance(Controller.class);
  
  private boolean autoPost;
  
  {
    // Add a handler to invoke the automatic post action after all the
    //   subcomponents have processed the RequestMessage
    addHandler
      (new AbstractMessageHandler()
      {
        @Override
        public void handleMessage(EventContext context, Message message,
            MessageHandlerChain next)
        { 
          next.handleMessage(context,message);
          if (autoPost && message.getType()==RequestMessage.TYPE)
          { createAction(context,false).invoke((ServiceContext) context);
          }
        }
      });
  }  

    
  /**
   * <p>Automatically run the "post" action before the "prepare" stage,
   *   regardless of whether or not the associated action appears in the 
   *   request line.
   * </p>
   * 
   * <p>Useful to trigger data updates and refreshes on each 
   *   page view.
   * </p>
   * 
   * @param autoPost
   */
  public void setAutoPost(boolean autoPost)
  { this.autoPost=autoPost;
  }
  
  @Override
  protected boolean wasActioned(ServiceContext context)
  { return autoPost || true;
  }
  
  @Override
  protected void handleRequest(ServiceContext context)
  {
    super.handleRequest(context);
    if (autoPost)
    { 
    }
  }
  
}
    

