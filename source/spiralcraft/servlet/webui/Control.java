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

import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.InitializeMessage;
import spiralcraft.textgen.Message;

import spiralcraft.textgen.compiler.TglUnit;
import spiralcraft.textgen.elements.Iterate;

import java.util.LinkedList;
import java.util.List;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Expression;

/**
 * <p>An element which accepts user input.
 * <p>
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
   *   control state.
   * </p>
   * 
   * @param context
   */
  public abstract void gather(ServiceContext context);
  
  public abstract String getVariableName();
  
  /**
   * <p>Read the value of the control from the data model target and apply
   *   it to the UI state.
   * </p> 
   * 
   * <p>Implementer should read the value of the control from the target
   *   Channel and buffer it in the control state for later rendering.
   * </p>
   * 
   * @param context
   */
  public abstract void scatter(ServiceContext context);
  
  
  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    
    if (expression!=null)
    { 
      Focus<?> parentFocus=getParent().getFocus();
      target=parentFocus.bind(expression);
    }
    controlGroupStateDistance=getParent().getStateDistance(ControlGroup.class);
    iterationStateDistance=getParent().getStateDistance(Iterate.class);
    bindChildren(childUnits);
    
  }
  
  @Override
  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    )
  {
    if (message.getType()==ControlMessage.TYPE)
    {
      // Scatter controls in pre-order, so containing elements can provide
      //   data for children to reference
      
      if (((ControlMessage) message).getOp()==ControlMessage.Op.SCATTER)
      { scatter((ServiceContext) context); 
      }
      
    } 
    else if (message.getType()==InitializeMessage.TYPE)
    {
      // Pre-scatter controls in pre-order, so containing elements can provide
      //   data for children to reference
      scatter((ServiceContext) context); 
    } 
    
    
    super.message(context,message,path);

    if (message.getType()==ControlMessage.TYPE)
    {
      // Read controls in post-order, so containing elements can process
      //   data children have gathered
      
      if (((ControlMessage) message).getOp()==ControlMessage.Op.GATHER)
      { gather((ServiceContext) context); 
      }
      
    } 
  }
  
  int getControlGroupStateDistance()
  { return controlGroupStateDistance;
  }

  int getIterationStateDistance()
  { return iterationStateDistance;
  }
}

