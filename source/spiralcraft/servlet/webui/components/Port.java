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
import spiralcraft.common.ContextualException;
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

  { alwaysRunHandlers=true;
  }
  
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
        =state.getPortSession(serviceContext);

      portCall.push(true);
      try
      { sequencer.service(serviceContext,Port.this,localSession,false);
      }
      finally
      { portCall.pop();
      }
    }
  }

  class PortContextHandler
    extends AbstractMessageHandler
  {
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
        =state.getPortSession(serviceContext);

      PortSession priorSession=serviceContext.getPortSession();
      serviceContext.setPortSession(localSession);
      try
      { next.handleMessage(dispatcher,message);
      }
      finally
      { serviceContext.setPortSession(priorSession);
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
  
  private final UISequencer sequencer=new UISequencer();
  
  protected final ThreadLocalStack<Boolean> portCall
    =new ThreadLocalStack<Boolean>();
  
  private boolean isolatePort;
  
  /**
   * Whether the message currently being processed was initiated by a call
   *   to this port.
   * 
   * @return
   */
  public boolean isPortCall()
  { return portCall.size()>0 && portCall.get();
  }
  
  @Override
  protected void addHandlers()
    throws ContextualException
  { 
    
    super.addHandlers();
    addExternalHandlers();
    addHandler(new PortHandler());
    addHandler(new BlockerHandler());
    addHandler(new PortContextHandler());
  }

  protected void addExternalHandlers()
    throws ContextualException
  { 
    
  }
  
  public void setIsolatePort(boolean isolatePort)
  { this.isolatePort=isolatePort;
  }
  
  
}
