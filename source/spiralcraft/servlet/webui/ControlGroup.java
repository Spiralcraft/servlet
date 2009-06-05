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

import java.util.LinkedList;
import java.util.List;

import java.io.IOException;

import spiralcraft.data.transaction.RollbackException;
import spiralcraft.data.transaction.Transaction;
import spiralcraft.data.transaction.TransactionException;
import spiralcraft.lang.BindException;

import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.AccessException;

import spiralcraft.lang.spi.AbstractChannel;


import spiralcraft.servlet.webui.ControlState.DataState;
import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;

import spiralcraft.textgen.compiler.TglUnit;

/**
 * <p>Groups a number of related Controls to create a single complex target
 *   value.
 * </p>
 * 
 * <p>A ControlGroup will pin a value and its state via a ThreadLocal, so that
 *   all of the ControlGroup's children have access to the same object
 *   via "callback" method calls or expressions.
 * </p>
 * 
 * 
 * 
 * @author mike
 * 
 */
public abstract class ControlGroup<Ttarget>
    extends Control<Ttarget>
{

  private int nextVariableName = 0;

  protected ThreadLocal<ControlGroupState<Ttarget>> threadLocalState 
    = new ThreadLocal<ControlGroupState<Ttarget>>();

  protected AbstractChannel<Ttarget> valueBinding;

  private Focus<?> focus;

  private String variableName;
  
  protected boolean useDefaultTarget=true;

  public ControlGroupState<Ttarget> getState()
  {
    return threadLocalState.get();
  }

  @Override
  public String getVariableName()
  {
    return variableName;
  }

  public String nextVariableName()
  {
    return Integer.toString(nextVariableName++);
  }

  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  public void message(EventContext context, Message message,
      LinkedList<Integer> path)
  {

    // Put our state into ThreadLocal storage so subcontrols can bind to
    // our value pinned to the Thread.
    
    boolean transactional=false;
    boolean newTransaction=false;
    if (message instanceof UIMessage)
    { transactional=((UIMessage) message).isTransactional();
    }
    
    Transaction transaction=null;
      
    if (transactional)
    {
      transaction
        =Transaction.getContextTransaction();
  
      if (transaction==null)
      { 
        transaction=
          Transaction.startContextTransaction(Transaction.Nesting.ISOLATE);
        newTransaction=true;
        if (debug)
        { logFine("Started new transaction");
        }
      }
      else
      {
        if (debug)
        { logFine("Obtained existing transaction");
        }
      }
      if (debug)
      { transaction.setDebug(true);
      }
    }
    
    try
    {
      
      ControlGroupState<Ttarget> state 
        = (ControlGroupState<Ttarget>) context.getState();
      if (threadLocalState.get() != state)
      {

        try
        {
          threadLocalState.set(state);
          super.message(context, message, path);
        } 
        finally
        {
          threadLocalState.remove();
        }
      }
      else
      {
        if (debug)
        { logFine("Re-entering message()");
        }
        // re-entrant mode
        super.message(context, message, path);
      }
      
      if (transaction!=null && state.isErrorState())
      { 
        if (debug)
        { logFine("Setting transaction rollbackOnComplete() due to error state");
        }
        transaction.rollbackOnComplete();
      }
      
      if (newTransaction)
      { transaction.commit();
      }
    }
    catch (RollbackException x)
    {
      if (debug)
      { logFine("Transaction rolled back");
      }
    }
    catch (TransactionException x)
    { handleException(context,x);
    }
    finally
    { 
      if (newTransaction)
      { transaction.complete();
      }
    }
      
  }

  
  /**
   * <p>Default implementation of render() for ControlGroup.
   * </p>
   * 
   * <p>Performs pre-order ThreadLocal state push, 
   *   and post-order ThreadLocal state pop()
   * </p>
   */
  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  public void render(EventContext context) throws IOException
  {
    if (debug)
    { logFine(toString()+": render");
    }
    
    try
    {
      threadLocalState.set((ControlGroupState<Ttarget>) context.getState());
      super.render(context);
    } 
    finally
    {
      threadLocalState.remove();
    }
  }

  /**
   * <p>Called once to allow a subclass to further extend the binding chain.
   *   The result of the binding will be pinned in ThreadLocalState in-between
   *   scatter() and gather() operations, and will be referred to in this
   *   component's Focus as exported to child Elements.
   * </p>
   * 
   * <p>This method is supports an extension mechanism by which subclasses
   *   may override this method to chain additional Channels to the one 
   *   returned by this default method implementation (the result of the "x" 
   *   expression or, if none is provided, the subject of the parent component's
   *   Focus).
   * </p>
   * 
   * 
   * @param parentFocus
   * @return The Channel that provides the target value to be stored in the
   *   state.
   * @throws BindException
   */
  protected Channel<?> bindTarget(Focus<?> parentFocus) throws BindException
  {
    if (expression != null)
    { return parentFocus.bind(expression);
    } 
    else if (useDefaultTarget)
    { return parentFocus.getSubject();
    }
    else
    { return null;
    }
  }

  /**
   * <p>Called once after the target has been bound and the local focus
   *   has been created to allow a subclass to create a new Focus to export
   *   to child Elements and to set up expressions based on the state-pinned
   *   value.
   * </p>
   * 
   * 
   * @return The new Focus to export, or null if the current Focus will be
   *   exported
   * @throws BindException 
   */
  protected Focus<?> bindExports()
    throws BindException
  { return null;
  }

  @Override
  /**
   * <p>Bind is made final here to allow the ControlGroup to maintain its
   * ThreadLocal state for access by child Controls.
   * </p>
   * 
   * <p>This method provides the value contained in the State as the default
   *   Focus available to child controls for binding expressions to.
   * </p>
   * 
   * <p>Override extend() to provide a source for the State value. This value
   *   will be pinned in the State from the "prepare" stage of one request
   *   up until the "prepare" stage of the next request, to ensure that any
   *   state sent to the browser can be modified and retrieved reliably. 
   * </p>
   * 
   * <p>Override bindSelf() to provide additional/different Channels to child
   *   components than the value stored in the State.
   * </p>
   * 
   * establish more specific Channels.
   */
  @SuppressWarnings("unchecked")
  // Not using generic versions
  public final void bind(List<TglUnit> childUnits) throws BindException,
      MarkupException
  {
    if (debug)
    { logFine(" bind():expression=" + expression);
    }
    Focus<?> parentFocus = getParent().getFocus();

    target = (Channel<Ttarget>) bindTarget(parentFocus);
    if (target != null)
    {
      valueBinding = new AbstractChannel<Ttarget>(target.getReflector())
      {
        @Override
        public Ttarget retrieve()
        {
          // log.fine(threadLocalState.get().toString());
          return threadLocalState.get().getValue();
        }

        @Override
        public boolean store(Ttarget val)
        {
          // log.fine("Store "+threadLocalState.get()+":"+val);
          threadLocalState.get().setValue(val);
          return true;
        }
      };
      
      // Always scatter on request if we can't cache the target
      if (UIServlet.cachingProhibited(target.getContentType()))
      { uncacheable=true;
      }
          
      // Expose the expression target as the new Focus, and add the
      // assembly in as another layer
      CompoundFocus myFocus = new CompoundFocus(parentFocus, valueBinding);
      myFocus.bindFocus("spiralcraft.servlet.webui", getAssembly().getFocus());
      focus=myFocus;
      bindRules(target.getReflector(),focus);
    } 
    else
    {
      // Expose the expression target as the new Focus, and add the
      // assembly in as another layer
      if (debug)
      { logFine("No Channel created, using parent focus");
      }
      CompoundFocus myFocus = new CompoundFocus(parentFocus, null);
      myFocus.bindFocus("spiralcraft.servlet.webui", getAssembly().getFocus());
      focus=myFocus;
      bindRules
        (((Channel<Ttarget>) parentFocus.getSubject()).getReflector()
        ,parentFocus
        );
    }
    if (variableName == null && getParent()!=null)
    {
      ControlGroup parentGroup = getParent().findElement(ControlGroup.class);
      if (parentGroup != null)
      { variableName = parentGroup.nextVariableName();
      }
      if (debug)
      { 
        log.debug
          ("Generating variable name '"+variableName+"' using parent "
              +parentGroup.toString()
          );
      }
    }
    computeDistances();
    Focus<?> newFocus=bindExports();
    if (newFocus!=null)
    { focus=newFocus;
    }
    bindChildren(childUnits);
  }

  /**
   * Specify the variable name for this ControlGroup, which will prefix
   *   the variable names of any contained Controls. If not supplied,
   *   a sequential name will be generated in the context of the parent
   *   control group.
   * 
   * @param variableName
   */
  public final void setVariableName(String variableName)
  { this.variableName=variableName;
  }
  
  @Override
  public Focus<?> getFocus()
  {
    return focus;
  }

  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  /**
   * Implements "scatter" by reading the value of the the bound channel
   *   into the state. Replaces any intermediate value with the bound
   *   value.
   */
  protected void scatter(ServiceContext context)
  {
    ControlGroupState<Ttarget> state = (ControlGroupState<Ttarget>) context
        .getState();

    try
    {
      if (target != null)
      {
        state.setValue(target.get());
        if (debug)
        { 
          String valueString
            =(state.getValue()!=null?state.getValue().toString():"null");
          if (valueString.length()>256)
          { valueString=valueString.substring(0,256)+"...";
          }
          logFine("Read value from target into state "+valueString);
        }
      }
      else
      {
        if (debug)
        { logFine("No target for control group, so not resetting state value");
        }
      }
      state.resetError();
      state.setDataState(DataState.SCATTERED);
    }
    catch (AccessException x)
    { handleException(context,x);
    }
    
  }

  @Override
  /**
   * <p>Finalized as unused because bind() is overridden
   * </p>
   */
  protected final void bindSelf()
  {
  }
  
  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  protected void gather(ServiceContext context)
  {
    ControlGroupState<Ttarget> state = (ControlGroupState<Ttarget>) context
        .getState();

    if (target != null)
    {
      try
      {
        if (target.isWritable())
        { target.set(state.getValue());
        }
        state.setDataState(DataState.GATHERED);
      } 
      catch (AccessException x)
      { handleException(context,x);
      }
    }

  }

  @Override
  public ControlGroupState<Ttarget> createState()
  {
    return new ControlGroupState<Ttarget>(this);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handleCommand(ServiceContext context)
  {
    ControlGroupState<Ttarget> state 
      = (ControlGroupState<Ttarget>) context.getState();
      
    while (true)
    {
      super.handleCommand(context);

      // Propogate messages sent by any executed Commands.
      //   We get our own message first.
  
      List<Message> messageList = state.dequeueMessages();

      if (messageList != null)
      {
        for (Message newMessage : messageList)
        {
          // Reentrant 
          message(context, newMessage, null);
        }
        continue;
      }
      else
      { 
        // End of queued messages
        break;
      }
    }
  }
}
