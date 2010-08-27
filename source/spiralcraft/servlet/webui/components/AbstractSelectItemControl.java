//
//Copyright (c) 1998,2008 Michael Toth
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


import spiralcraft.util.string.StringConverter;

import spiralcraft.textgen.EventContext;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ServiceContext;

/**
 * <p>Manages an single selection from an AbstractSelectControl
 * </p>
 * 
 * <p>Specify an expression for "value" property to define the key that
 *   gets sent back to the target of the AbstractSelectControl. This expression
 *   is defined with respect to the target "x" property, which is usually an
 *   item from the collection defined in the AbstractSelectControl "source" 
 *   property.
 *   
 * </p>
 * 
 * <p>The entry presented to the user is defined in a child control or subtype. 
 *   This control will export the value of the "x" target property, which is
 *   usually an item from the collection defined in the AbstractSelectControl
 *   "source" property. Specify the the optional "x" target property to provide
 *   an alternate / more specific choice representation.
 * </p>
 * 
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
public class AbstractSelectItemControl<Ttarget,Tvalue>
  extends Control<Ttarget>
{
  
  protected StringConverter<Tvalue> converter;
  private Expression<Tvalue> valueExpression;
  private Channel<Tvalue> value;
  
  public void setValue(Expression<Tvalue> valueExpression)
  { this.valueExpression=valueExpression;
  }
  
  
  @Override
  public Focus<?> bindSelf(Focus<?> focus)
    throws BindException
  {
    if (valueExpression==null)
    { 
      throw new BindException
        (getClass().getName()
        +": 'value' property must be assigned an expression"
        );
    }
    value=focus.bind(valueExpression);
    if (converter==null && value!=null)
    { converter=value.getReflector().getStringConverter();
    }
    if (debug && target==null)
    { log.fine("Not bound to anything");
    }
    return focus;
  }
  
  @Override
  public String getVariableName()
  { return null;
  }
  
  @Override
  public SelectItemState<Tvalue> createState()
  { return new SelectItemState<Tvalue>(this);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected SelectItemState<Tvalue> getState(EventContext context)
  { return (SelectItemState<Tvalue>) context.getState();
  }
  
  @Override
  public void gather(ServiceContext context)
  {
    // SelectList does all the gathering here.
  }
  
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    SelectItemState<Tvalue> state=((SelectItemState<Tvalue>) context.getState());
    if (value!=null)
    {

      
      try
      { 
        Tvalue val=value.get();
        if (debug)
        { log.fine(getLogPrefix()+" scattering "+val);
        }
        state.updateValue(val);
        state.setSelected(state.getSelectState().isSelected(state.getValue()));
      }
      catch (AccessException x)
      { handleException(context,x);
      }
      catch (NumberFormatException x)
      { handleException(context,x);
      }

      
    }    
    
  }
  

  
}



