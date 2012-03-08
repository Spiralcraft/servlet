//
//Copyright (c) 1998,2012 Michael Toth
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
package spiralcraft.servlet.webui.kit;



import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.kit.AbstractMessageHandler;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.servlet.webui.PortMessage;
import spiralcraft.servlet.webui.ServiceContext;

public class PortHandler
  extends AbstractMessageHandler
{
  
  private Component component;
  private final UISequencer sequencer=new UISequencer();

  { type=PortMessage.TYPE;
  }
  
  
  @Override
  public Focus<?> bind(Focus<?> chain) 
    throws BindException
  { 
    component
      =LangUtil.assertInstance(Component.class,chain);
    return chain;
  }
  
  @Override
  protected void doHandler(
    Dispatcher dispatcher,
    Message message,
    MessageHandlerChain next)
  { 
    ServiceContext serviceContext
      =(ServiceContext) dispatcher;
    
    PortSession localSession
      =((ComponentState) serviceContext.getState()).getPortSession();

    sequencer.service(serviceContext,component,localSession);
  }
  
  
  
}
