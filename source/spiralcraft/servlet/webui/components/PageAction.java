//
//Copyright (c) 1998,2008 Michael Toth
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
import spiralcraft.command.Command;
import spiralcraft.common.ContextualException;
import spiralcraft.util.Sequence;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;

import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;


import spiralcraft.textgen.EventContext;



/**
 * <p>Registers a command to be triggered by the 'action' URI query parameter
 * </p>
 * 
 * <p>The command is executed in the "prepare" stage of the request,
 *   because the action is not a direct user response to a previously rendered
 *   page state.
 * </p>
 * 
 * <p>If triggering behavior on a form post is desired, use a FormCommand
 *   instead.
 * </p>
 * 
 * @author mike
 *
 */
public class PageAction
  extends Component
{
  
  private Expression<Command<?,?,?>> commandExpression;
  private Channel<Command<?,?,?>> commandChannel;  
  private String actionName;
  private boolean queue=false;

  public void setX(Expression<Command<?,?,?>> expression)
  { commandExpression=expression;
  }
  
  public void setActionName(String name)
  { actionName=name;
  }

  /**
   * <p>Queue the command. When in a form, this is a good idea. If
   *   not in a form, it will never run if queued. Defaults to true,
   *   for compatibility, but in the future will be auto-set.
   * </p>
   * @param queue
   */
  public void setQueue(boolean queue)
  { this.queue=queue;
  }
  
  @Override
  protected void handleInitialize(ServiceContext context)
  {
    super.handleInitialize(context);
    if (actionName!=null)
    { context.registerAction(createAction(context));
    }
    
  }
  
  @Override
  protected void handleRequest(ServiceContext context)
  {
    super.handleRequest(context);
    if (actionName==null)
    { context.registerAction(createAction(context));
    }
    
  }

  @Override
  protected void handlePrepare(ServiceContext context)
  {
    super.handlePrepare(context);
    if (actionName==null)
    { context.registerAction(createAction(context));
    }
    
    ActionState state=getState(context);
    
    Command<?,?,?> command=state.dequeueCommand();
    if (command!=null)
    { 
      log.warning
        ("Unexecuted command on Prepare "+command+"."
        +" Result visibility may be delayed"
        );
      state.queueCommand(command);
    }
    
  }
  
  @Override
  protected void handleCommand(ServiceContext context)
  {
  
    Command<?,?,?> command=getState(context).dequeueCommand();
    if (command!=null)
    {
      if (debug)
      { log.fine("Executing action command for action "+actionName);
      }
      command.execute();
      if (command.getException()!=null)
      { command.getException().printStackTrace();
      }
    }
    
  }
  
  @Override
  public Focus<?> bindStandard(Focus<?> focus)
    throws ContextualException
  { 
    if (commandExpression!=null)
    { commandChannel=focus.bind(commandExpression);
    }
    return super.bindStandard(focus);
  }  
  
  /**
   * <p>Create an Action target
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  protected Action createAction(EventContext context)
  {
    String actionName=this.actionName;
    if (actionName==null)
    { 
      Sequence<Integer> path=context.getState().getPath();
      actionName=path.format("."); 
    }
    
    return new Action(actionName,context.getState().getPath())
    {

      { responsive=PageAction.this.actionName==null;
      }
      
     
      @Override
      public void invoke(ServiceContext context)
      { 
        if (PageAction.this.debug)
        { log.fine("Action invoked "+getName());
        }
        if (commandChannel!=null)
        {
          Command<?,?,?> command=commandChannel.get();
          if (command instanceof QueuedCommand)
          { 
            if (debug)
            { log.fine("Executing "+command);
            }
            // We should run queued commands immediately, so they are not
            //   double-queued.
            command.execute();
          }
          else
          {
            if (queue)
            {
              if (debug)
              { log.fine("Queueing "+command);
              }
              // We'll execute the command at the Command stage.
              getState(context).queueCommand(commandChannel.get());
            }
            else
            { 
              if (debug)
              { log.fine("Executing "+command);
              }
              command.execute();
              if (command.getException()!=null)
              { command.getException().printStackTrace(); 
              }
            }
          }
        }
      }
      
    };
  }  
  
  @Override
  public ActionState createState()
  { return new ActionState(this);
  }
  
  @Override
  protected ActionState getState(Dispatcher context)
  { return (ActionState) context.getState();
  }
  
}

class ActionState
  extends ComponentState
{
  private Command<?,?,?> command;
  
  public ActionState(PageAction comp)
  { super(comp);
  }
  
  public void queueCommand(Command<?,?,?> command)
  { this.command=command;
  }
  
  public Command<?,?,?> dequeueCommand()
  { 
    try
    { return command;
    }
    finally
    { command=null;
    }
  }
}

