//
//Copyright (c) 2012 Michael Toth
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

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.kit.AbstractMessageHandler;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.servlet.webui.PortMessage;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.kit.PortSession;
import spiralcraft.servlet.webui.kit.UISequencer;
import spiralcraft.util.thread.ThreadLocalStack;

public class Port
  extends Component
{

  
  class PortHandler
    extends AbstractMessageHandler
  {
    { type=PortMessage.TYPE;
    }
    
    @Override
    protected void doHandler(
      Dispatcher dispatcher,
      Message message,
      MessageHandlerChain next)
    { 
      ServiceContext serviceContext
        =(ServiceContext) dispatcher;
    
      ComponentState state=(ComponentState) serviceContext.getState();
    
      PortSession localSession
        =state.getPortSession();

      portCall.push(true);
      try
      { sequencer.service(serviceContext,Port.this,localSession);
      }
      finally
      { portCall.pop();
      }
    }
  }

  /**
   * Blocks all messages when we're not being called as a port
   * 
   * @author mike
   */
  class BlockerHandler
    extends AbstractMessageHandler
  { 
    
    @Override
    protected void doHandler(
      Dispatcher dispatcher,
      Message message,
      MessageHandlerChain next)
    { 
      if (!isolatePort || (portCall.size()>0 && portCall.get()))
      { next.handleMessage(dispatcher,message);
      }
    }
  }
  
  private UISequencer sequencer=new UISequencer();
  
  private ThreadLocalStack<Boolean> portCall
    =new ThreadLocalStack<Boolean>();
  
  private boolean isolatePort;
  
  { 
    addHandler(new PortHandler());
    addHandler(new BlockerHandler());
  }
  
  public void setIsolatePort(boolean isolatePort)
  { this.isolatePort=isolatePort;
  }
  
  
}
