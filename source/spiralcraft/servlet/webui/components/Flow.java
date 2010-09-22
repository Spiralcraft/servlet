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
package spiralcraft.servlet.webui.components;

import java.util.LinkedList;

import spiralcraft.command.AbstractCommandFactory;
import spiralcraft.command.CommandAdapter;
import spiralcraft.command.Command;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.EventContext;

/**
 * 
 * Implements a state-transition model- INCOMPLETE
 * 
 * @author mike
 */
public class Flow<Tstate,Tinput>
  extends ControlGroup<Tstate>
{
  
  public final AbstractCommandFactory<Flow<Tstate,Tinput>,Tinput,Void> 
    queueInput
    =new AbstractCommandFactory<Flow<Tstate,Tinput>,Tinput,Void>()
  {
    @Override
    public Command<Flow<Tstate,Tinput>,Tinput,Void> command()
    { 
      return new CommandAdapter<Flow<Tstate,Tinput>,Tinput,Void>()
      {
        @Override
        protected void run()
        { getState().queueInput(getContext());
        }
      };
    }
  };
  
  
  @Override
  public ControlGroupState<Tstate> createState()
  { return new FlowState<Tstate,Tinput>(this);
  } 
  
  
  @Override
  public void scatter(ServiceContext context)
  {
    FlowState<Tstate,Tinput> state=getState(context);
    Tinput input=null;
    while ((input=state.dequeueInput())!=null)
    { 
      
    }
    
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public FlowState<Tstate,Tinput> getState()
  { return (FlowState<Tstate,Tinput>) super.getState();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public FlowState<Tstate,Tinput> getState(EventContext context)
  { return (FlowState<Tstate,Tinput>) context.getState();
  }
  
}

class FlowState<Tstate,Tinput>
  extends ControlGroupState<Tstate>
{
  
  private LinkedList<Tinput> input;
  
  public FlowState(Flow<Tstate,Tinput> flow)
  { super(flow);
  }
  
  public void queueInput(Tinput input)
  {
    if (this.input==null)
    { this.input=new LinkedList<Tinput>();
    }
    this.input.add(input);
  }
  
  public Tinput dequeueInput()
  { 
    if (this.input!=null)
    { 
      Tinput ret=this.input.removeFirst();
      if (this.input.isEmpty())
      { this.input=null;
      }
      return ret;
    }
    return null;
  }
}