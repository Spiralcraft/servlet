//
//Copyright (c) 1998,2009 Michael Toth
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
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.PrepareMessage;
import spiralcraft.textgen.ValueState;


/**
 * <p>Publishes a value into the context for the duration of the session.
 * </p> 
 * 
 * @author mike
 *
 * @param <T>
 */
public class Session<T>
  extends spiralcraft.textgen.elements.Session<T>
{
  private RequestBinding<?>[] requestBindings;
  
  { addHandler(new AbstractMessageHandler()
      {
        { this.type=PrepareMessage.TYPE;
        }

        @Override
        protected void doHandler(
          Dispatcher dispatcher,
          Message message,
          MessageHandlerChain next)
        {
          if (requestBindings!=null)
          { 
            if (debug)
            { log.fine("Publishing request bindings");
            }
            for (RequestBinding<?> binding: requestBindings)
            { binding.publish(ServiceContext.get());
            }
          }
          
          next.handleMessage(dispatcher,message);
        }
        
      }
    );
  }
  
  public void setRequestBindings(RequestBinding<?>[] requestBindings)
  { this.requestBindings=requestBindings;
  }
  
  @Override
  protected void onFrameChange(Dispatcher context)
  {
    if (requestBindings!=null)
    {
      for (RequestBinding<?> requestBinding:requestBindings)
      { requestBinding.read(ServiceContext.get().getQuery());
      }
    }
  }
  
  @Override
  protected Focus<?> bindExports(Focus<?> focusChain) throws BindException
  {     
    
    if (requestBindings!=null)
    { 
      for (RequestBinding<?> requestBinding:requestBindings)
      { requestBinding.bind(focusChain);
      }
    }
    return focusChain;
    
  }
  
  
  @Override
  protected T computeExportValue(ValueState<T> state)
  {
    T value=super.computeExportValue(state);
    
    
    // Apply bindings here
    return value;
    
  }
  
}

