//
//Copyright (c) 1998,2007 Michael Toth
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
package spiralcraft.servlet.webui;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.log.ClassLog;

/**
 * Defers the execution of a Command by placing into a state queue to allow
 *   the target to execute it an an appropriate time.
 *   
 * @author mike
 *
 */
public class QueuedCommand<Ttarget,Tcontext,Tresult>
  extends CommandAdapter<Ttarget,Tcontext,Tresult>
{
  private static final ClassLog log
    =ClassLog.getInstance(QueuedCommand.class);
  private static boolean debug;
  
  private final ControlGroupState<Ttarget> state;
  private final Command<Ttarget,Tcontext,Tresult> command;
  
  public QueuedCommand
    (ControlGroupState<Ttarget> state
    ,Command<Ttarget,Tcontext,Tresult> command
    )
  { 
    this.state=state;
    this.command=command;
  }
  
  public Command <Ttarget,Tcontext,Tresult> getCommand()
  { return command;
  }
  
  @Override
  public final void run()
  { 
    if (debug)
    { log.fine("Queueing "+command);
    }
    state.queueCommand(command);
  }
  
  @Override
  public String toString()
  { return super.toString()+" queues: "+command.toString();
  }
  
}
