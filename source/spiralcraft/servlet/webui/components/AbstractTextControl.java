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

import java.util.List;

import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.compiler.TglUnit;
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
  
  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    super.bind(childUnits);
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
          converter=new ArrayToString(targetClass.getComponentType());
        }
      }
      else
      {
        converter=
          (StringConverter<Ttarget>) 
          StringConverter.getInstance(target.getContentType());
      }
    }
    if (target==null)
    { log.fine(getLogPrefix()+"Not bound to anything (formvar name="+name+")");
    }
  }
  
  @Override
  public ControlState<String> createState()
  { return new ControlState<String>(this);
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    ControlState<String> state=((ControlState<String>) context.getState());
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
          { state.setValue(converter.toString(val));
          }
          else
          { state.setValue(val.toString());
          }
        }
        else
        { state.setValue(null);
        }
      }
      catch (AccessException x)
      { state.setException(x);
      }
      catch (NumberFormatException x)
      { state.setException(x);
      }

      
    }
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    ControlState<String> state=((ControlState<String>) context.getState());
    //System.err.println("TextInput: readPost");
      
    // Only update if changed
    if (context.getPost()!=null)
    {
    
      String postVal=context.getPost().getOne(state.getVariableName());
      if (debug)
      { log.fine(getLogPrefix()+"Got posted value "+postVal);
      }
      
      if (postVal==null && !state.getPresented())
      { 
        if (debug)
        { log.fine(getLogPrefix()+"Ignoring not-presented control");
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
      else if (state.updateValue(postVal))
      {
        if (target!=null)
        {
          
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
            if (inspect(tval,state))
            { target.set(tval);
            }
          
          }
          catch (AccessException x)
          { state.setException(x);
          }
          catch (NumberFormatException x)
          { state.setException(x);
          }
          catch (IllegalArgumentException x)
          { state.setException(x);
          }

        }
      }
    }

  }
  

}
