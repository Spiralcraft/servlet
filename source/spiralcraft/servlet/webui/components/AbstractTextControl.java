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


import spiralcraft.common.ContextualException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.kit.Callable;
import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.util.string.ArrayToString;
import spiralcraft.util.string.StringArrayToString;
import spiralcraft.util.string.StringConverter;

/**
 * <p>Provides common functionality associated with controls that manipulate
 *   textual representations of form data.
 * </p>
 * 
 * @author mike
 *
 * @param <Ttarget>
 */
public abstract class AbstractTextControl<Ttarget>
  extends Control<Ttarget>
{
  private String name;
  private StringConverter<Ttarget> converter;
  private boolean required;
  private Binding<?> onInput;
  private Callable<Ttarget,?> onInputFn;
//  private boolean ignoreEmptyInput;
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setRequired(boolean required)
  { this.required=required;
  }

  @Override
  public String getVariableName()
  { return name;
  }
  
  public void setConverter(StringConverter<Ttarget> converter)
  { this.converter=converter;
  }
  
  public void setOnInput(Binding<?> onInput)
  { this.onInput=onInput;
  }
  
//  /**
//   * Ignores empty input
//   * 
//   * @param ignoreEmptyInput
//   */
//  public void setIgnoreEmptyInput(boolean ignoreEmptyInput)
//  { this.ignoreEmptyInput=ignoreEmptyInput;
//  }
  
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" }) // Not using generic versions
  public Focus<?> bindSelf(Focus<?> focus)
    throws ContextualException
  { 
    if (converter==null && target!=null)
    { 
      Class targetClass=target.getContentType();
      if (targetClass.isArray())
      { 
        if (targetClass.getComponentType().equals(String.class))
        { 
          converter=(StringConverter<Ttarget>) new StringArrayToString();
          ((StringArrayToString) converter).setTrim(true);
        }
        else
        { 
          converter=target.getReflector().getStringConverter();
          if (converter==null)
          {
            if (targetClass.getComponentType().isPrimitive())
            { 
              throw new BindException
                ("No automatic conversion from text to "
                  +targetClass.getComponentType()+" is defined. Converter"
                  +" must be provided for form field "+getVariableName()
                );
            }
            else
            {
              // XXX Use better method that integrates target channel
              converter=new ArrayToString(targetClass.getComponentType());
              log.info
                ("Using default comma delimited format to translate array for "
                +"form field "+getVariableName()+" of type "
                +target.getReflector().getTypeURI()
                );
            }
          }
        }
      }
      else
      { converter=target.getReflector().getStringConverter();
      }
      
      if (onInput!=null)
      { onInputFn=new Callable(getSelfFocus(),target.getReflector(),onInput);
      }

    }
    if (target==null)
    { log.fine(getLogPrefix()+"Not bound to anything (formvar name="+name+")");
    }
    return focus;
  }
  
  @Override
  public ControlState<String> createState()
  { return new ControlState<String>(this);
  }
  
  
  @Override
  public void scatter(ServiceContext context)
  {
    ControlState<String> state=getState(context);
    if (target!=null)
    {
      try
      {
        Ttarget val=target.get();
        if (debug)
        { log.fine(getLogPrefix()+" scattering "+val);
        }
        if (val!=null)
        {
          
          if (converter!=null)
          { state.updateValue(converter.toString(val));
          }
          else
          { state.updateValue(val.toString());
          }
        }
        else
        { state.updateValue(null);
        }
      }
      catch (AccessException x)
      { handleException(context,x);
      }
      catch (NumberFormatException x)
      { handleException(context,x);
      }

      
    }
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    ControlState<String> state=getState(context);
    //System.err.println("TextInput: readPost");
      
    // Only update if changed
    if (context.getPost()!=null)
    {
    
      String postVal=context.getPost().getFirst(state.getVariableName());
      if (debug)
      { log.fine(getLogPrefix()+"Got posted value "+postVal);
      }
      
      if (postVal==null && !state.getPresented())
      { 
        if (debug)
        { 
          log.fine
            (getLogPrefix()+"Ignoring missing value for not-presented control");
        }
        return;
      }
      
      
      // Empty strings should be null.
      if (postVal!=null && postVal.length()==0)
      { postVal=null;
      }

      if (required && postVal==null)
      { 
        
        if (debug)
        { log.fine(getLogPrefix()+"Failed required test");
        }
        state.addError("Input required");
      }
      else 
      {
        state.setValue(postVal);
        try
        {
          
          String val=state.getValue();
        
          Ttarget tval=null;
          if (converter!=null && val!=null)
          { tval=converter.fromString(val);
          }
          else
          { tval=(Ttarget) val;
          }
            
          String previousVal=state.getPreviousValue();
          Ttarget tpreviousVal=null;
          if (converter!=null && previousVal!=null)
          { tpreviousVal=converter.fromString(previousVal);
          }
          else
          { tpreviousVal=(Ttarget) previousVal;
          }
            
          if (inspect(tval,state))
          { 
            if (conditionallyUpdateTarget(tval,tpreviousVal))
            { state.valueUpdated();
            }
          }
          
          if (onInputFn!=null)
          { onInputFn.evaluate(tval);
          }
        }
        catch (AccessException x)
        { handleException(context,x);
        }
        catch (NumberFormatException x)
        { handleException(context,x);
        }
        catch (IllegalArgumentException x)
        { handleException(context,x);
        }

      }
    }

  }
  

}
