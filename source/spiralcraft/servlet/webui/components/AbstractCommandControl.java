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



import java.util.LinkedList;

import spiralcraft.command.Command;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.servlet.webui.Control;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;
import spiralcraft.lang.BindException;

/**
 * Generic control fires Commands from within a WebUI component tree
 * 
 * @author mike
 *
 */
public abstract class AbstractCommandControl<Tcontext,Tresult>
  extends Control<Command<?,Tcontext,Tresult>>
{  
  
  protected Binding<Tcontext> contextX;
  protected ThreadLocalChannel
    <Command<?,Tcontext,Tresult>>commandLocal;
  
  protected Focus<?> focus;
  
  @Override
  public void message
    (EventContext context,Message message,LinkedList<Integer> path)
  { 
    try
    { 
      pushState(context);
      
      super.message(context,message,path);
    
    }
    finally
    { popState(context);
    }
  }
      
  public void setContextX(Binding<Tcontext> contextX)
  { this.contextX=contextX;
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
  protected void executeCommand(EventContext context)
  {    
    try
    {
      final CommandState<Tcontext,Tresult> state=getState(context);
    
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
        { handleException(context,command.getException());
        }
      }
    }
    catch (AccessException x)
    { handleException(context,x);
    }
      
  }
  
  @Override
  protected void bindSelf()
    throws BindException
  {
    Focus<?> focusChain=super.getFocus();
    if (contextX!=null)
    { contextX.bind(focusChain);
    }
    commandLocal
      =new ThreadLocalChannel<Command<?,Tcontext,Tresult>>
        (target.getReflector());
    this.focus=focusChain.chain(commandLocal);
  }
  
  @Override
  public Focus<?> getFocus()
  { return focus;
  }
  
  @Override
  public CommandState<Tcontext,Tresult> createState()
  { return new CommandState<Tcontext,Tresult>(this);
  }  
  

  @SuppressWarnings("unchecked")
  @Override
  protected CommandState<Tcontext,Tresult> getState(EventContext context)
  { return (CommandState<Tcontext,Tresult>) context.getState();
  }
  
  protected final void pushState(EventContext context)
  { commandLocal.push(getState(context).getValue());
  }
  
  protected final void popState(EventContext context)
  { commandLocal.pop();
  }
}


