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
import java.util.LinkedList;
import java.util.List;

import java.io.IOException;

import spiralcraft.command.Command;
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
public class ControlGroup<Ttarget>
  extends Control<Ttarget>
{
  @SuppressWarnings("unused")
  private static final ClassLogger log=new ClassLogger(ControlGroup.class);
  private int nextVariableName=0;
  
  protected ThreadLocal<ControlGroupState<Ttarget>> threadLocalState
    =new ThreadLocal<ControlGroupState<Ttarget>>();
  
  protected AbstractChannel<Ttarget> valueBinding;
  
  private CompoundFocus<Ttarget> focus;
  
  private String variableName;

  
  public ControlGroupState<Ttarget> getState()
  { return threadLocalState.get();
  }
  
  public String getVariableName()
  { return variableName;
  }
  
  public String nextVariableName()
  { return Integer.toString(nextVariableName++);
  }
  
  @SuppressWarnings("unchecked") // Blind cast
  @Override
  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    )
  {

    try
    {
      threadLocalState.set((ControlGroupState<Ttarget>) context.getState());
      super.message(context,message,path);
    }
    finally
    { threadLocalState.remove();
    }
  }
  
  @SuppressWarnings("unchecked") // Blind cast
  @Override
  public void render
    (EventContext context
    )
    throws IOException
  {

    try
    {
      threadLocalState.set((ControlGroupState<Ttarget>) context.getState());
      super.render(context);
    }
    finally
    { threadLocalState.remove();
    }
  }

  protected Channel<?> bind(Focus<?> parentFocus)
    throws BindException
  { 
    if (expression!=null)
    { return parentFocus.<Ttarget>bind(expression);
    }
    else
    { return null;
    }
  }
  
  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    System.err.println("ControlGroup.bind() :expression="+expression);
    Focus<?> parentFocus=getParent().getFocus();

    
    target=(Channel<Ttarget>) bind(parentFocus);
    if (target!=null)
    { 
      valueBinding=new AbstractChannel<Ttarget>(target.getReflector())
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
      //   assembly in as another layer
      focus=new CompoundFocus(parentFocus,valueBinding);  
      focus.bindFocus("spiralcraft.servlet.webui",getAssembly().getFocus());
    }
    else 
    {
      // Expose the expression target as the new Focus, and add the 
      //   assembly in as another layer
      focus=new CompoundFocus(parentFocus,null);  
      focus.bindFocus("spiralcraft.servlet.webui",getAssembly().getFocus());

    }
    if (variableName==null)
    { 
      ControlGroup parentGroup=this.findElement(ControlGroup.class);
      if (parentGroup!=null)
      { variableName=parentGroup.nextVariableName();
      }
    }
    
    computeDistances();
    bindChildren(childUnits);
  }
  
  public Focus<Ttarget> getFocus()
  { return focus;
  }

  @SuppressWarnings("unchecked") // Blind cast
  @Override
  public void scatter(ServiceContext context)
  { 
    ControlGroupState<Ttarget> state=
      (ControlGroupState<Ttarget>) context.getState();
    
    if (target!=null)
    { state.setValue(target.get());
    }
    state.setError(null);
    state.setErrorState(false);
    
  }

  @SuppressWarnings("unchecked") // Blind cast
  @Override
  public void gather(ServiceContext context)
  { 
    ControlGroupState<Ttarget> state=
      (ControlGroupState<Ttarget>) context.getState();
    
    
    if (target!=null)
    {
      try
      { 
        target.set(state.getValue());
        executeCommands(state);
      }
      catch (AccessException x)
      { state.setError(x.getMessage());
      }
    }
    
    
    
  }

  public ControlGroupState<Ttarget> createState()
  { return new ControlGroupState<Ttarget>(this);
  }
  
  /**
   * Execute a command- by callback only in the message and render chain
   * 
   * @param <X>
   * @param command
   * @return
   */
  public <X> X executeCommand(Command<Ttarget,X> command)
  { 
    log.fine(command.toString());
    command.setTarget(getState().getValue());
    command.execute();
    return command.getResult();
  }

  private void handleException(Exception exception)
  {
    // Make exception available
    exception.printStackTrace();
    getState().setError(exception.toString());
  }
  
  private void executeCommands(ControlGroupState<Ttarget> state)
  {
    List<Command<Ttarget,?>> commands=state.dequeueCommands();
    if (commands!=null)
    {
      for (Command<Ttarget,?> command : commands)
      { 
        executeCommand(command);
        if (command.getException()!=null)
        { handleException(command.getException());
        }
        
      }
    }
  }
}
