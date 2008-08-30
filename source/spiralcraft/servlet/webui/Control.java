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

import spiralcraft.rules.Inspector;
import spiralcraft.rules.Rule;
import spiralcraft.rules.RuleSet;
import spiralcraft.rules.Violation;
import spiralcraft.servlet.webui.ControlState.DataState;
import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.EventContext;

import spiralcraft.textgen.Message;

import spiralcraft.textgen.compiler.TglUnit;
import spiralcraft.textgen.elements.Iterate;
import spiralcraft.util.ArrayUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


import spiralcraft.command.Command;
import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Setter;
import spiralcraft.log.ClassLogger;

/**
 * <P>An Component that coordinates or provides a means of user input.
 * </P>
 * 
 * 
 * 
 * @author mike
 *
 */
public abstract class Control<Ttarget>
  extends Component
{
  protected static final ClassLogger log=ClassLogger.getInstance(Control.class);
  
  protected Channel<Ttarget> target;
  protected Expression<Ttarget> expression;
  protected int controlGroupStateDistance=-1;
  protected int iterationStateDistance=-1;
  
  protected RuleSet<Control<Ttarget>,Ttarget> ruleSet;
  protected Inspector<Control<Ttarget>,Ttarget> inspector;
  
  public void setX(Expression<Ttarget> x)
  { this.expression=x;
  }
  
  /**
   * <p>Read the value of the control from the input context and apply it 
   *   to the data model target
   * </p> 
   * 
   * <p>Implementer should read the value of the control and apply it to
   *   the target Channel. The control value should be buffered in the
   *   control state to prevent input data loss if the target Channel
   *   rejects the value.
   * </p>
   * 
   * <p>gather() is normally called from message() in post-order, ie.
   *   after all children have been gather()ed, which ensures that the
   *   child elements of a composite value have been read so that the
   *   fully composed value can be applied to the data model.
   * </p>
   * 
   * <p>For example, a date control will have its component month, day and
   *   year gather()ed via child components before the date value is
   *   computed and applied to the data model.
   * </p>
   * 
   * @param context
   */
  protected abstract void gather(ServiceContext context);

  
  public abstract String getVariableName();
  
  /**
   * <p>Read the value of the control from the data model target and apply
   *   it to the UI state.
   * </p> 
   * 
   * <p>This method is called from the default implementation of handleRequest()
   *   if there the state is not in an error state
   * </p>
   * 
   * <p>Implementer should read the value of the control from the target
   *   Channel and buffer it in the control state for later rendering.
   * </p>
   * 
   * <p>scatter() is normally called from message() in pre-order, ie.
   *   before children have been scatter()ed, which ensures that a
   *   composite value has been read from a data model before the
   *   individual elements of the value are associated with child controls
   * </p>
   * 
   * <p>For example, a date control will read the date from the data model
   *   in its scatter() method, before the child controls that display or
   *   allow modification of the month, day and year are associated with
   *   their values in their own scatter() method.
   *
   * @param context
   */
  protected abstract void scatter(ServiceContext context);
  
  @Override
  @SuppressWarnings("unchecked")
  protected void handlePrepare(ServiceContext context)
  { 
    ControlState<Ttarget> state = (ControlState) context.getState();
    if (!state.isErrorState())
    { 
      if (debug)
      { log.fine("Calling SCATTER: "+this+" state="+state);
      }
      scatter(context);
    }
    else
    { 
      if (debug)
      {
        log.fine("NOT Calling SCATTER bc error state: "
          +this+" state="+state
          +" errors="+ArrayUtil.format(state.getErrors(),",",null)
          +" exception="+state.getException()
        );
      }

    }
    
  }
  
  @Override
  public ControlState<?> createState()
  { return new ControlState<Ttarget>(this);
  }
  
  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    
    Focus<?> parentFocus=getParent().getFocus();
    if (expression!=null)
    { 
      target=parentFocus.bind(expression);
    }
    else
    { 
      target=(Channel<Ttarget>) parentFocus.getSubject();
      if (target==null)
      { target=(Channel<Ttarget>) parentFocus.getContext();
      }
    }
    computeDistances();
    if (ruleSet!=null)
    {
      inspector
        =ruleSet.bind
          (target.getReflector()
          ,parentFocus
          );      
    }
    bindChildren(childUnits);
    
  }
  
  /**
   * <p>Specify any rules that apply to the data value managed by this
   *   control.
   * </p>
   * 
   * @param rules
   */
  public void setRules(Rule<Control<Ttarget>,Ttarget>[] rules)
  {
    if (ruleSet==null)
    { ruleSet=new RuleSet<Control<Ttarget>,Ttarget>(this);
    }
    ruleSet.addRules(rules);
  }
  
  /**
   * <p>Inspect the value and process any Rule Violations, returning "true"
   *   if there are no Rule Violations.
   * </p>
   * 
   * @param value
   * @return
   */
  protected boolean inspect(Ttarget value,ControlState<?> state)
  {
    if (inspector==null)
    { return true;
    }
    Violation<Ttarget>[] violations=inspector.inspect(value);
    if (violations==null)
    { return true;
    }
    
    for (Violation<Ttarget> violation : violations)
    { state.addError(violation.getMessage());
    }
    return false;
  }
  
  protected void computeDistances()
  {
    controlGroupStateDistance=getParent().getStateDistance(ControlGroup.class);
    iterationStateDistance=getParent().getStateDistance(Iterate.class);
  }
  
  @Override
  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    )
  {
    
    try
    { super.message(context,message,path);
    }
    catch (RuntimeException x)
    { 
      ((ControlState<?>) context.getState()).setException(x);
      throw x;
    }

    if (message.getType()==GatherMessage.TYPE)
    {
      // Read controls in post-order, so containing elements can process
      //   data children have gathered
      gather((ServiceContext) context); 
    } 

  }
  
  public ControlGroup<?> getControlGroup()
  { 
    if (getParent()!=null)
    { return getParent().findElement(ControlGroup.class);
    }
    else
    { return null;
    } 
      
  }
  
  /**
   * 
   * @return The number of states between this Control's parent and
   *   a ControlGroup- for fast reference to this state's containing
   *   ControlGroup
   */
  int getControlGroupStateDistance()
  { return controlGroupStateDistance;
  }

  int getIterationStateDistance()
  { return iterationStateDistance;
  }



  /**
   * Execute any commands that have been queued. 
   * 
   * Commands provide an opportunity to implement application behaviors once
   *   all data in the state tree has been gathered.   
   * 
   * @param context
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void handleCommand(ServiceContext context)
  {
    ControlState<Ttarget> state=((ControlState<Ttarget>) context.getState());
    
    List<Command<Ttarget,?>> commands
      =state.dequeueCommands();
    
    while (commands!=null)
    {
      for (Command<Ttarget,?> command : commands)
      { 
        if (debug)
        { log.fine("Executing "+command.toString());
        }
        command.setTarget(state.getValue());

        command.execute();
        
        if (command.getException()!=null)
        { 
          state.setException(command.getException());
          log.log
            (Level.FINE,"Command threw exception "+command.toString()
            ,command.getException()
            );
        }
          
      }
      commands=state.dequeueCommands();
    }
    
  }
  
  
  /**
   * <p>Default implementation of render() for Controls.
   * </p>
   * 
   * <p>Performs pre-order internal state check, debug trap, 
   *   and post-order ControlState error state reset.
   * </p>
   */
  @Override
  @SuppressWarnings("unchecked")
  public void render(EventContext context)
    throws IOException
  {    
    

    ControlState<Ttarget> state=(ControlState<Ttarget>) context.getState();
    if (state.getControl()!=this)
    { throw new RuntimeException("State tree out of sync "+state+" "+this);
    }
    if (debug)
    { log.fine("Control.render() "+this+" state="+state);
    }
    
    super.render(context);
    state.setDataState(DataState.RENDERED);
    state.resetError();
  }
  
  protected Setter<?>[] bindAssignments(Assignment<?>[] assignments)
    throws BindException
  {
    if (assignments!=null)
    {
      Setter<?>[] setters=new Setter<?>[assignments.length];
      int i=0;
      for (Assignment<?> assignment: assignments)
      { setters[i++]=assignment.bind(getFocus());
      }
      return setters;
    }
    return null;
  }

}

