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

import java.util.ArrayList;
import java.util.List;

import spiralcraft.command.Command;

/**
 * Represents the state of ControlGroup, which associates a single value
 *   with multiple form elements.
 * 
 * @author mike
 */
public class ControlGroupState<Tbuf>
  extends ControlState<Tbuf>
{
  private int localName=0;
  private boolean errorState=false;
  private ArrayList<Command<Tbuf,?>> commands;
  
  
  public ControlGroupState(ControlGroup<?> controlGroup)
  { super(controlGroup);
  }
  
  /**
   * 
   * @return The next autogenerated local name for child controls
   */
  public String nextLocalName()
  { return Integer.toString(localName++);
  }
  
  public boolean isErrorState()
  { return errorState;
  }
  
  public void setErrorState(boolean errorState)
  {
    this.errorState=errorState;
    if (errorState && controlGroupState!=null)
    { controlGroupState.setErrorState(true);
    }
  }
 
  /**
   * Queue a Command for execution after the "gather" phase of
   *   reading the browser input.
   * 
   * @param command
   */
  public synchronized void queueCommand(Command<Tbuf,?> command)
  { 
    if (commands==null)
    { commands=new ArrayList<Command<Tbuf,?>>(1);
    }
    commands.add(command);
  }
  
  public List<Command<Tbuf,?>> dequeueCommands()
  { 
    List<Command<Tbuf,?>> list=commands;
    commands=null;
    return list;
  }
}
