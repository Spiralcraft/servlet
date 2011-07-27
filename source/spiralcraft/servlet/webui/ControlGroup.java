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

import java.util.List;


import spiralcraft.common.ContextualException;
import spiralcraft.data.transaction.RollbackException;
import spiralcraft.data.transaction.Transaction;
import spiralcraft.data.transaction.TransactionException;
import spiralcraft.lang.BindException;

import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.SimpleFocus;

import spiralcraft.lang.spi.AbstractChannel;



import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;


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


  
  protected AbstractChannel<Ttarget> valueBinding;

  private String variableName;
  
  protected boolean useDefaultTarget=true;
  private Binding<?> afterCommitX;

  public void setAfterCommitX(Binding<?> afterCommitX)
  { 
    this.removeTargetContextual(this.afterCommitX);
    this.addTargetContextual(afterCommitX);
    this.afterCommitX=afterCommitX;
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

  @Override
  public void message(Dispatcher context, Message message)
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
      

      super.message(context, message);

      ControlGroupState<Ttarget> state =getState(context);      
      if (transaction!=null && state.isErrorState())
      { 
        if (debug)
        { logFine("Setting transaction rollbackOnComplete() due to error state");
        }
        transaction.rollbackOnComplete();
      }
      
      if (newTransaction)
      { 
        transaction.commit();

        if (afterCommitX!=null)
        { 
          try
          { afterCommitX.get();
          }
          catch (Exception x)
          { handleException(context,x);
          }
        }
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
  protected Channel<?> bindTarget(Focus<?> parentFocus) 
    throws ContextualException
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
  protected Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  { return focus;
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
   * <p>Override bindExports() to provide additional/different Channels to child
   *   components than the value stored in the State.
   * </p>
   * 
   * establish more specific Channels.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  // Not using generic versions
  public final Focus<?> bind(Focus<?> focus) 
      throws ContextualException
  {
    try
    {
      
      if (debug)
      { logFine(" bind():expression=" + expression);
      }
  
      bindParentContextuals(focus);
      
      target = (Channel<Ttarget>) bindTarget(focus);
      if (target != null)
      {
        valueBinding = new AbstractChannel<Ttarget>(target.getReflector())
        {
          { origin=target;
          }
          
          @Override
          public Ttarget retrieve()
          {
            // log.fine(threadLocalState.get().toString());
            final ControlGroupState<Ttarget> state=getState();
            if (state!=null)
            { return state.getValue();
            }
            else
            { 
              throw new IllegalStateException
                ("Not in context: "
                +ControlGroup.this.toString()
                +": "+ControlGroup.this.getErrorContext()
                );
            }
          }
  
          @Override
          /**
           * Buffer the value updated by any child objects
           */
          public boolean store(Ttarget val)
          {
            // log.fine("Store "+threadLocalState.get()+":"+val);
            getState().setValue(val);
            return true;
          }
        };
        
        // Always scatter on request if we can't cache the target
        if (UIServlet.cachingProhibited(target.getContentType()))
        { uncacheable=true;
        }
            
        // Expose the expression target as the new Focus, and add the
        // assembly in as another layer
        SimpleFocus myFocus = new SimpleFocus(focus, valueBinding);
        bindContextuals(myFocus,targetContextuals);
        bindSelfFocus(focus);
  
        myFocus.addFacet(getAssembly().getFocus());
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
        bindContextuals(focus,targetContextuals);
        SimpleFocus myFocus = new SimpleFocus(focus, null);
        bindSelfFocus(focus);
        myFocus.addFacet(getAssembly().getFocus());
        focus=myFocus;      
        bindRules
          (((Channel<Ttarget>) focus.getSubject()).getReflector()
          ,focus
          );
      }
      bindHandlers(focus);
      if (variableName == null && getParent()!=null)
      {
        ControlGroup parentGroup = getParent().findComponent(ControlGroup.class);
        if (parentGroup != null)
        { 
          
          variableName = parentGroup.nextVariableName();
          if (debug)
          { 
            log.debug
              ("Generating variable name '"+variableName+"' using parent "
                  +parentGroup.toString()
              );
          }
        }
      }
      computeDistances();
      focus=bindExports(focus);
  
      bindChildren(focus);
      return focus;
    }
    catch (ContextualException x)
    { throw new ContextualException("Bind error",getErrorContext(),x);
    }
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
  /**
   * Implements "scatter" by reading the value of the the bound channel
   *   into the state. Replaces any intermediate value with the bound
   *   value.
   */
  protected void scatter(ServiceContext context)
  {
    ControlGroupState<Ttarget> state = getState(context);

    try
    {
      if (target != null)
      {
        Ttarget lastValue=state.getValue();
        state.updateValue(target.get());
        if (debug)
        { 
          String valueString
            =(state.getValue()!=null?state.getValue().toString():"null");
          String lastValueString
            =(lastValue!=null?lastValue.toString():"null");
          
          logFine("Read value from target into state ["+valueString+"] old value was ["+lastValueString+"]");
        }
      }
      else
      {
        if (debug)
        { logFine("No target for control group, so not resetting state value");
        }
      }
      state.resetError();
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
  protected final Focus<?> bindSelf(Focus<?> focus)
  { return focus;
  }
  
  @Override
  protected void gather(ServiceContext context)
  {
    ControlGroupState<Ttarget> state = getState(context);

    if (target != null)
    {
      try
      {
        
        if (conditionallyUpdateTarget
              (state.getValue(),state.getPreviousValue())
           )
        { state.valueUpdated();
        }
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
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public ControlGroupState<Ttarget> getState()
  { return (ControlGroupState) super.getState();
  }
  
  
  @SuppressWarnings("unchecked")
  @Override
  protected  <X> ControlGroupState<X> getState(Dispatcher context)
  { return (ControlGroupState<X>) context.getState();
  }
  
  @Override
  public void handleCommand(ServiceContext context)
  {
    ControlGroupState<Ttarget> state = getState(context);
      
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
          context.dispatch(newMessage,this,null);
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
