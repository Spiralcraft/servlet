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

import spiralcraft.lang.BindException;

import spiralcraft.lang.Focus;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.WriteException;

import spiralcraft.lang.spi.AbstractBinding;

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

  protected ThreadLocal<ControlGroupState<Ttarget>> threadLocalState
    =new ThreadLocal<ControlGroupState<Ttarget>>();
  
  protected AbstractBinding<Ttarget> valueBinding;
  
  private CompoundFocus<Ttarget> focus;

  public ControlGroupState<Ttarget> getState()
  { return threadLocalState.get();
  }
  
  public String getVariableName()
  { return null;
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

  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    System.err.println("ControlGroup.bind() :expression="+expression);
    Focus<?> parentFocus=getParent().getFocus();

    
    if (expression!=null)
    { 
      target=parentFocus.bind(expression);
      valueBinding=new AbstractBinding<Ttarget>(target.getReflector())
      {
        public Ttarget retrieve()
        {
          return threadLocalState.get().getValue();
        }
        
        public boolean store(Ttarget val)
        {
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
      { target.set(state.getValue());
      }
      catch (WriteException x)
      { state.setError(x.getMessage());
      }
    }
    
  }

  public ControlGroupState<Ttarget> createState()
  { return new ControlGroupState<Ttarget>(this);
  }
}
