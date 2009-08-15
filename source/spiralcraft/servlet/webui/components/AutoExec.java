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



import spiralcraft.command.Command;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

/**
 * <p>Automatically runs a command whenever the state frame changes.
 * </p>
 * 
 * <p>The command is either executed in the "request" or the "prepare"
 *   stage, depending on which stage encounters a state frame change, and
 *   whether the "whenX" expression returns "true" if supplied.
 * </p>
 * 
 * @author mike
 *
 */
public class AutoExec
  extends Control<Command<?,?>>
{
  

  private Expression<Boolean> whenX;
  private Channel<Boolean> whenChannel; 
  private boolean delayUntilPrepare;
  
  
  public void setWhenX(Expression<Boolean> whenX)
  { this.whenX=whenX;
  }
  
  /**
   * Ensures that the command will only run at the "prepare" stage of
   *   the request. If the request is the first in a sequence, the command
   *   will run after all processing has been completed in the "request"
   *   stage.
   * 
   * @param delayUntilPrepare
   */
  public void setDelayUntilPrepare(boolean delayUntilPrepare)
  { this.delayUntilPrepare=delayUntilPrepare;
  }

  @Override
  public void handleRequest(ServiceContext context)
  {
    
    // Make sure we bypass the command if we're delaying it
    ((CommandState) context.getState()).setBypass(delayUntilPrepare);
    super.handleRequest(context);
  }
  
  
  @Override
  public void handlePrepare(ServiceContext context)
  {
    if (delayUntilPrepare)
    { 
      // If we bypassed the command on "request", make sure we don't bypass
      //   it now.
      ((CommandState) context.getState()).setBypass(false);
    }
    super.handleRequest(context);
  }
  
  
  @Override
  protected void scatter(ServiceContext context)
  { runCommand(context);    
  }

  
  @Override
  public String getVariableName()
  { return null;
  }
  
  protected void runCommand(ServiceContext context)
  {
    CommandState state=(CommandState) context.getState();
    if (state.getBypass())
    {
      // Run the command only once during a cycle
      return;
    }
  
    if (whenChannel==null || Boolean.TRUE.equals(whenChannel.get()))
    {
      Command<?,?> command=target.get();
      if (command!=null)
      {
        if (debug)
        { log.fine("Executing AutoCommand  "+command);
        }
        command.execute();
        state.setBypass(true);
        if (command.getException()!=null)
        { 
          ((ControlState<?>) context.getState())
            .setException(command.getException());
        }
      }
    }
    
  }
  
  
  @Override
  public void bindSelf()
    throws BindException
  { 
    
    super.bindSelf();
    if (whenX!=null)
    { whenChannel=getFocus().bind(whenX);
    }
  }  

  @Override
  protected void gather(ServiceContext context)
  { 
    // We're not doing anything here
   
    
  }  

  @Override
  public CommandState createState()
  { return new CommandState(this);
  }
  
}

class CommandState
  extends ControlState<Command<?,?>>
{
  private boolean bypass;
  
  public CommandState(AutoExec comp)
  { super(comp);
  }
  
  public void setBypass(boolean bypass)
  { this.bypass=bypass;
  }
  
  public boolean getBypass()
  { return bypass;
  }
  
}



