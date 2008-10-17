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

import java.util.List;

import spiralcraft.command.Command;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;

import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.ElementState;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.util.ArrayUtil;

import spiralcraft.log.ClassLogger;

/**
 * <p>Registers a command to be triggered by the 'action' URI query parameter
 * </p>
 * 
 * <p>The command is executed in the "prepare" stage of the request,
 *   because the action is not a direct user response to a previously rendered
 *   page state.
 * </p>
 * 
 * @author mike
 *
 */
public class PageAction
  extends Component
{
  
  private static final ClassLogger log
    =ClassLogger.getInstance(PageAction.class);
  
  private Expression<Command<?,?>> commandExpression;
  private Channel<Command<?,?>> commandChannel;  
  private String actionName;

  public void setX(Expression<Command<?,?>> expression)
  { commandExpression=expression;
  }
  
  public void setActionName(String name)
  { actionName=name;
  }
  
  @Override
  protected void handleInitialize(ServiceContext context)
  {
    super.handleInitialize(context);
    if (actionName!=null)
    { 
      context.registerAction(createAction(context));
      if (debug)
      { log.fine("Registered action "+actionName);
      }
    }

    
  }
  
  @Override
  protected void handlePrepare(ServiceContext context)
  {
    super.handlePrepare(context);
    if (actionName==null)
    { context.registerAction(createAction(context));
    }
    
  }
  
  @Override
  protected void handleCommand(ServiceContext context)
  {
  
    Command<?,?> command=((ActionState) context.getState()).dequeueCommand();
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
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    
    Focus<?> parentFocus=getParent().getFocus();
    if (commandExpression!=null)
    { commandChannel=parentFocus.bind(commandExpression);
    }

    bindChildren(childUnits);
    
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
      int[] path=context.getState().getPath();
      actionName=ArrayUtil.format(path,".",null); 
    }
    
    return new Action(actionName,context.getState().getPath())
    {

      { clearable=PageAction.this.actionName==null;
      }
      
     
      @Override
      public void invoke(ServiceContext context)
      { 
        if (PageAction.this.debug)
        { log.fine("Action invoked "+getName());
        }
        if (commandChannel!=null)
        {
          Command<?,?> command=commandChannel.get();
          if (command instanceof QueuedCommand)
          { 
            // We should run queued commands immediately, so they are not
            //   double-queued.
            command.execute();
          }
          else
          {
            // We'll execute the command at the Command stage.
            ((ActionState) context.getState())
              .queueCommand(commandChannel.get());
          }
        }
      }
      
    };
  }  
  
  @Override
  public ElementState createState()
  { return new ActionState(this);
  }
  
}

class ActionState
  extends ElementState
{
  private Command<?,?> command;
  
  public ActionState(PageAction comp)
  { super(comp.getChildCount());
  }
  
  public void queueCommand(Command<?,?> command)
  { this.command=command;
  }
  
  public Command<?,?> dequeueCommand()
  { 
    try
    { return command;
    }
    finally
    { command=null;
    }
  }
}

