//
//Copyright (c) 2009 Michael Toth
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




import spiralcraft.command.Command;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.servlet.webui.Control;
import spiralcraft.task.Eval;
import spiralcraft.app.Dispatcher;
import spiralcraft.lang.BindException;

import spiralcraft.app.Message;

/**
 * Generic control fires Commands from within a WebUI component tree
 * 
 * @author mike
 *
 */
public abstract class AbstractActionControl<Tcontext,Tresult>
  extends Control<Command<?,Tcontext,Tresult>>
{  
  
  protected Binding<Tcontext> contextX;
  protected ThreadLocalChannel
    <Command<?,Tcontext,Tresult>>commandLocal;
  protected Binding<Void> onSuccess;
  protected Binding<Void> onError;
  protected Focus<?> focus;
  protected Expression<Tresult> onAction;
  
  
  @Override
  public void message
    (Dispatcher context,Message message)
  { 
    try
    { 
      pushState(context);
      
      super.message(context,message);
    
    }
    finally
    { popState(context);
    }
  }
  
  /**
   * The expression to fire when the component is actioned. Use in place
   *   of the 'x' property to evaluate an expression instead of firing 
   *   a Command.
   *   
   * @param onAction
   */
  public void setOnAction(Expression<Tresult> onAction)
  { this.onAction=onAction;
  }
  
  public void setContextX(Binding<Tcontext> contextX)
  { this.contextX=contextX;
  }
  
  public void setOnSuccess(Binding<Void> onSuccess)
  { this.onSuccess=onSuccess;
  }
  
  public void setOnError(Binding<Void> onError)
  { this.onError=onError;
  }

  @Override
  protected Channel<Command<?,Tcontext,Tresult>> 
    resolveDefaultTarget(Focus<?> focus)
      throws ContextualException
  {
    if (onAction!=null)
    { 
      Eval<Tcontext,Tresult> eval=new Eval<Tcontext,Tresult>(onAction);
      eval.bind(focus);
      return new SimpleChannel<Eval<Tcontext,Tresult>>(eval,true)
        .resolve(focus,"command",new Expression[0]);
      
    }
    return null;
  }
  
  /**
   * <p>Obtain a new Command instance from the target channel or other source,
   *   apply any specified contextual parameters, execute the command, 
   *   and publish the completed Command in the state for access
   *   by child components.
   * </p>
   *  
   * 
   * @param context
   */
  protected void fireAction(Dispatcher context)
  {    
    try
    {
      final ActionControlState<Tcontext,Tresult> state=getState(context);
      
      
      Command<?,Tcontext,Tresult> command=state.getValue();
      if (command==null 
          || command.isStarted()
          || command.isCompleted()
          )
      { 
        command=null;
        if (target!=null)
        { 
          command=target.get();
          if (contextX!=null)
          { command.setContext(contextX.get());
          }
          state.setValue(command);
          commandLocal.set(command);
        }
      }
    
      if (command!=null)
      {
        
        if (debug)
        { logFine("Executing Command  "+command);
        }
        command.execute();
      
        if (command.getException()!=null)
        { 
          if (onError!=null)
          { onError.get();
          }
          handleException(context,command.getException());
        }
        else
        { 
          if (onSuccess!=null)
          { onSuccess.get();
          }
        }
      }
    }
    catch (AccessException x)
    { handleException(context,x);
    }
      
  }
  
  @Override
  protected Focus<?> bindSelf(Focus<?> focus)
    throws ContextualException
  {
    if (!(Command.class.isAssignableFrom(target.getContentType()))

        && ((Class<?>) target.getContentType())!=Void.class
        && ((Class<?>) target.getContentType())!=Void.TYPE
       )
    { 
      throw new BindException
        ("Channel does not provide a command: "
        +target.getReflector().getTypeURI()
        );
    }
    else if (target==null)
    { 
      throw new BindException
        ("Command must be provided via 'x' property or by "
        +" containing component"
        );
    }
    
    if (contextX!=null)
    { contextX.bind(focus);
    }
    commandLocal
      =new ThreadLocalChannel<Command<?,Tcontext,Tresult>>
        (target.getReflector());
    focus=focus.chain(commandLocal);
    if (onSuccess!=null)
    { onSuccess.bind(focus);
    }
    if (onError!=null)
    { onError.bind(focus);
    }
    return focus;
  }
  

  
  @Override
  public ActionControlState<Tcontext,Tresult> createState()
  { return new ActionControlState<Tcontext,Tresult>(this);
  }  
  

  @SuppressWarnings("unchecked")
  @Override
  protected ActionControlState<Tcontext,Tresult> getState(Dispatcher context)
  { return (ActionControlState<Tcontext,Tresult>) context.getState();
  }
  
  protected final void pushState(Dispatcher context)
  { commandLocal.push(getState(context).getValue());
  }
  
  protected final void popState(Dispatcher context)
  { commandLocal.pop();
  }
}


