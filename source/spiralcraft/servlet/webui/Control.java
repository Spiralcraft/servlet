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


import spiralcraft.command.Command;
import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.Setter;
import spiralcraft.log.Level;

/**
 * <p>A Component that provides a view or an interaction point bound a model
 *   object specified by an expression
 * </p>
 * 
 * 
 * 
 * @author mike
 *
 */
public abstract class Control<Ttarget>
  extends Component
{
  protected Channel<Ttarget> target;
  protected Expression<Ttarget> expression;
  protected int controlGroupStateDistance=-1;
  protected int iterationStateDistance=-1;
  
  protected RuleSet<Control<Ttarget>,Ttarget> ruleSet;
  protected Inspector<Control<Ttarget>,Ttarget> inspector;
  protected boolean scatterOnRequest;
  protected boolean scatterOnPrepare=true;
  protected boolean uncacheable;
  
  private boolean contextualizeName=true;
  
  /**
   * <p>The binding target expression that represents the "model" that this
   *   Control provides a view for.
   * </p>
   * 
   * @param x
   */
  public void setX(Expression<Ttarget> x)
  { this.expression=x;
  }
  
  
  /**
   * <p>Indicates whether the variable name for this Control (which is used
   *   by the client to uniquely identify it for data binding purposes- eg.
   *   accepting a form post), should be contextualized (ie. given an 
   *   automatically generated prefix) to permit multiple instances of the
   *   control in the same unit (eg. a form) to function properly.
   * </p>
   * 
   * <p>Defaults to true. Set to false if the control must be addressed using a
   *   pre-determined name, as may be the case in non-responsive client
   *   requests.
   * </p>
   * 
   * @param contextualizeName
   */
  public void setContextualizeName(boolean contextualizeName)
  { this.contextualizeName=contextualizeName;
  }
  
  /**
   * 
   * @return Whether or not the variable name output in markup will be
   *   contextualized
   *   (automatically prefixed) to permit multiple instances.
   *   
   */
  public boolean getContextualizeName()
  { return contextualizeName;
  }
  
  /**
   * <p>Specify that this control should ALWAYS consider the contents of 
   *   its source expression during the REQUEST phase, before any actions are
   *   handled. This is useful for components that need an immediately 
   *   up-to-date value against which to process user actions.
   * </p>
   * 
   * <p>Normally, scatter() is called during the REQUEST phase ONLY for
   *   non-responsive requests- ie. not a link-back or post-back. scatter()
   *   is usually called during the PREPARE phase, after actions complete
   *   and before rendering takes place.
   * </p>
   * 
   * @param val
   */
  public void setScatterOnRequest(boolean val)
  { this.scatterOnRequest=val;
  }
  
  /**
   * <p>Specify whether this control should consider the contents of 
   *   its source expression during the PREPARE phase, after any actions are
   *   handled. Typically, when Controls are read, values are updated and 
   *   scatter() must be called to refresh the state tree.
   * </p>
   * 
   * <p>The default value of this property is true. If set to false, scatter()
   *   will not be called on prepare. This is useful when the source value
   *   for the control is expensive to compute and not likely to change
   *   independently of the user interface.
   * </p>
   * 
   * @param val
   */
  public void setScatterOnPrepare(boolean val)
  { this.scatterOnPrepare=val;
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
   * <p>If the request is not responsive (ie. not in response to a previous
   *   rendering), this method is called from the default implementation of 
   *   handleRequest() if the state is not in an error state, in order to
   *   establish an initial state for a sequence of interations.
   * </p>
   * 
   * <p>This method is also called from the default implementation of 
   *   handlePrepare() if the state is not in an error state
   * </p>

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
  protected void handleRequest(ServiceContext context)
  { 
    ControlState<Ttarget> state = (ControlState) context.getState();
    if (state.frameChanged(context.getCurrentFrame()))
    {
      if (debug)
      { logFine("Scattering on Request due to frame change : state="+state);
      }
      state.resetError();
      scatter(context);
    }
    else if (scatterOnRequest || uncacheable)
    {
      if (!state.isErrorState())
      { 
        if (debug)
        { logFine("Calling SCATTER: state="+state);
        }
        scatter(context);
      }
      else
      { 
        if (uncacheable)
        {
          if (debug)
          { 
            logFine
              ("Calling SCATTER b/c uncacheable target "
              +target.getContentType().getName()
              +": state="+state
              );
          }
          scatter(context);
        }
        else
        {
          if (debug)
          {
            logFine
              ("NOT Calling SCATTER bc error state: "
              +" state="+state
              +" errors="+ArrayUtil.format(state.getErrors(),",",null)
              +" exception="+state.getException()
              );
          }
        }

      }
    }
    
  }
  
  @Override
  @SuppressWarnings("unchecked")
  protected void handlePrepare(ServiceContext context)
  { 
    ControlState<Ttarget> state = (ControlState) context.getState();
    boolean frameChanged=state.frameChanged(context.getCurrentFrame());
    
    if (frameChanged && debug)
    { log.fine("Frame changed on PREPARE");
    }
    
    if (frameChanged || scatterOnPrepare)
    {
      state.setPresented(false);
      if (!state.isErrorState())
      { 
        if (debug)
        { logFine("Calling SCATTER: "+this+" state="+state);
        }
        scatter(context);
      }
      else
      { 
        if (debug)
        {
          logFine
          ("NOT Calling SCATTER bc error state: "
          +" state="+state
          +" errors="+ArrayUtil.format(state.getErrors(),",",null)
          +" exception="+state.getException()
          );
        }

      }
    }
    else
    { 
      if (debug)
      {
        logFine
          ("Not scattering- scatterOnPrepare=false"
          );
      }
    }
    
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected void postPrepare(ServiceContext context)
  { 
   
    ControlState<Ttarget> state = (ControlState<Ttarget>) context.getState();
    List<Command<Ttarget,?>> commands=state.dequeueCommands();
    if (commands!=null && commands.size()>0)
    {
      for (Command<Ttarget,?> command : commands)
      {
        log.warning
          ("Unexecuted command queued post-prepare- results may not "
          +" be rendered: "+command
          );
        state.queueCommand(command);
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
    bindSelf();
    if (target!=null)
    { bindRules(target.getReflector(),getFocus());
    }
    else if (getFocus().getSubject()!=null)
    { 
      bindRules
        ((Reflector<Ttarget>) getFocus().getSubject().getReflector()
        ,getFocus()
        );
    }
    bindChildren(childUnits);
    
  }
  
  /**
   * <p>Override to bind anything and set this component's focus. Called from
   *   bind() before rules are bound.
   * </p>
   * @throws BindException
   */
  protected void bindSelf()
    throws BindException
  { }
  
  protected void bindRules(Reflector<Ttarget> reflector,Focus<?> focus)
    throws BindException
  {
    if (ruleSet!=null)
    {
      inspector
        =ruleSet.bind
          (reflector
          ,focus
          );      
    }
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
    if (debug)
    { logFine("Inspecting "+value);
    }
    Violation<Ttarget>[] violations=inspector.inspect(value);
    if (violations==null)
    { return true;
    }
    
    for (Violation<Ttarget> violation : violations)
    { 
      if (debug)
      { logFine("Failed inspection "+violation.getMessage());
      }
      state.addError(violation.getMessage());
    }
    return false;
  }
  
  protected void computeDistances()
  {
    controlGroupStateDistance=getParent().getStateDistance(ControlGroup.class);
    iterationStateDistance=getParent().getStateDistance(Iterate.class);
  }
  
  @SuppressWarnings("unchecked") // State cast
  @Override
  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    )
  {

    if (message.getType()==GatherMessage.TYPE)
    {
      // Reset error in pre-order, so controls can raise it if needed.
      ((ControlState<Ttarget>) context.getState()).resetError(); 
    } 
    
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
        { logFine("Executing "+command.toString());
        }
        command.setTarget(state.getValue());

        command.execute();
        
        if (command.getException()!=null)
        { handleException(context,command.getException());
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
    { logFine("Control.render() "+this+" state="+state);
    }
    
    super.render(context);
    state.setDataState(DataState.RENDERED);
  }
  
  protected Setter<?>[] bindAssignments(Assignment<?>[] assignments)
    throws BindException
  {
    if (assignments!=null)
    {
      Setter<?>[] setters=new Setter<?>[assignments.length];
      int i=0;
      for (Assignment<?> assignment: assignments)
      { 
        setters[i]=assignment.bind(getFocus());
        if (debug)
        { setters[i].setDebug(true);
        }
        i++;
      }
      return setters;
    }
    return null;
  }

  protected void logFine(String message)
  { log.log(Level.FINE,getLogPrefix()+": "+message,null,1);
  }

  protected void logWarning(String message)
  { log.warning(getLogPrefix()+": "+message);
  }
  
  protected void logInfo(String message)
  { log.info(getLogPrefix()+": "+message);
  }

  protected String getLogPrefix()
  {
    
    String id=getId();
    String variableName=getVariableName();
    return toString()+(id!=null?"id="+id+": ":"")+"name="+variableName+" ";
    
    
  }  
  
  /**
   * <p>Call to handle an exception. Standard behavior is to put exception in
   *   the state and log it.
   * </p>
   * @param context
   * @param state
   * @param x
   */
  protected void handleException
    (EventContext context
    ,Exception x
    )
  {
    ((ControlState<?>) context.getState()).setException(x);
    logHandledException(context,x);
  }
  
}

