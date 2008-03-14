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

import spiralcraft.data.transaction.Transaction;
import spiralcraft.data.transaction.TransactionException;
import spiralcraft.lang.BindException;

import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.AccessException;

import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.log.ClassLogger;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;

import spiralcraft.textgen.compiler.TglUnit;

/**
 * Groups a number of related Controls to create a single complex target value
 * 
 * @author mike
 * 
 */
public abstract class ControlGroup<Ttarget>
    extends Control<Ttarget>
{
  @SuppressWarnings("unused")
  private static final ClassLogger log = ClassLogger.getInstance(ControlGroup.class);

  private int nextVariableName = 0;

  protected ThreadLocal<ControlGroupState<Ttarget>> threadLocalState = new ThreadLocal<ControlGroupState<Ttarget>>();

  protected AbstractChannel<Ttarget> valueBinding;

  private Focus<?> focus;

  private String variableName;

  public ControlGroupState<Ttarget> getState()
  {
    return threadLocalState.get();
  }

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
        // re-entrant mode
        super.message(context, message, path);
      }
      
      if (newTransaction)
      { transaction.commit();
      }
    }
    catch (TransactionException x)
    { getState().setException(x);
    }
    finally
    { 
      if (newTransaction)
      { transaction.complete();
      }
    }
      
  }

  
  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  public void render(EventContext context) throws IOException
  {

    try
    {
      threadLocalState.set((ControlGroupState<Ttarget>) context.getState());
      super.render(context);
    } finally
    {
      threadLocalState.remove();
    }
  }

  /**
   * Called once to allow subclass to further extend the binding chain. The
   * result of the binding will be cached in ThreadLocalState in-between
   * scatter() and gather() operations, and will be referred to in this
   * component's Focus as exported to child Elements.
   * 
   * @param parentFocus
   * @return
   * @throws BindException
   */
  protected Channel<?> extend(Focus<?> parentFocus) throws BindException
  {
    if (expression != null)
    {
      return parentFocus.<Ttarget> bind(expression);
    } else
    {
      return null;
    }
  }

  /**
   * Called after the local focus has been create to allow other expressions to
   * be bound
   * 
   * @param thisFocus
   */
  protected Focus<?> bindSelf()
    throws BindException
  { return null;
  }

  @Override
  /**
   * Bind is made final here to allow the ControlGroup to maintain its
   * ThreadLocal state for access by child Controls. Override bind(Focus) to
   * establish more specific Channels.
   */
  @SuppressWarnings("unchecked")
  // Not using generic versions
  public final void bind(List<TglUnit> childUnits) throws BindException,
      MarkupException
  {
    log.fine(getClass().getName() + ".bind():expression=" + expression);
    Focus<?> parentFocus = getParent().getFocus();

    target = (Channel<Ttarget>) extend(parentFocus);
    if (target != null)
    {
      valueBinding = new AbstractChannel<Ttarget>(target.getReflector())
      {
        public Ttarget retrieve()
        {
          // log.fine(threadLocalState.get().toString());
          return threadLocalState.get().getValue();
        }

        public boolean store(Ttarget val)
        {
          // log.fine("Store "+threadLocalState.get()+":"+val);
          threadLocalState.get().setValue(val);
          return true;
        }
      };

      // Expose the expression target as the new Focus, and add the
      // assembly in as another layer
      CompoundFocus myFocus = new CompoundFocus(parentFocus, valueBinding);
      myFocus.bindFocus("spiralcraft.servlet.webui", getAssembly().getFocus());
      focus=myFocus;
    } 
    else
    {
      // Expose the expression target as the new Focus, and add the
      // assembly in as another layer
      log.fine("No Channel created, using parent focus: for "
          + getClass().getName());
      CompoundFocus myFocus = new CompoundFocus(parentFocus, null);
      myFocus.bindFocus("spiralcraft.servlet.webui", getAssembly().getFocus());
      focus=myFocus;
    }
    if (variableName == null)
    {
      ControlGroup parentGroup = this.findElement(ControlGroup.class);
      if (parentGroup != null)
      {
        variableName = parentGroup.nextVariableName();
      }
    }
    computeDistances();
    Focus<?> newFocus=bindSelf();
    if (newFocus!=null)
    { focus=newFocus;
    }
    bindChildren(childUnits);
  }

  public Focus<?> getFocus()
  {
    return focus;
  }

  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  public void scatter(ServiceContext context)
  {
    ControlGroupState<Ttarget> state = (ControlGroupState<Ttarget>) context
        .getState();

    if (target != null)
    {
      state.setValue(target.get());
    }
    state.setError(null);
    state.setErrorState(false);

  }

  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  public void gather(ServiceContext context)
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
      } 
      catch (AccessException x)
      {
        state.setError(x.getMessage());
      }
    }

  }

  public ControlGroupState<Ttarget> createState()
  {
    return new ControlGroupState<Ttarget>(this);
  }

  @SuppressWarnings("unchecked")
  public void command(ServiceContext context)
  {
    while (true)
    {
      super.command(context);

      // Propogate messages sent by any executed Commands.
      //   We get our own message first.
      ControlGroupState<Ttarget> state 
        = (ControlGroupState<Ttarget>) context.getState();
  
      List<Message> messageList = state.dequeueMessages();
      if (messageList != null)
      {
        for (Message newMessage : messageList)
        {
          message(context, newMessage, null);
        }
      }
      else
      { 
        // End of queued messages
        break;
      }
    }
  }
}
