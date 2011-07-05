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


import spiralcraft.util.string.StringConverter;


import spiralcraft.app.Dispatcher;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.net.http.VariableMapBinding;

import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ServiceContext;

/**
 * <p>Controls the selection of one or multiple values 
 * </p>
 * 
 * <p>The "x" (binding target) property contains an expression that references
 *   the currently selected item(s).
 * </p>
 * 
 * <p>The optional "source" property contains an 
 *   expression that provides a list of candidate values, used for generating 
 *   the set of options (see the Option class). If a source is provided,
 *   it will be exported to child components via the Focus chain.
 *   
 * </p>
 * 
 * <p>In the single-select case, the gather method expects a single post 
 *   variable that represents the selected value, to be converted from
 *   a string by the specified converter object.
 * </p>
 *  
 * <p>In the multi-select case, the gather method expects a post variable
 *   with multiple values that represents the selected values, where 
 *   each value is converted from a string by the specified converter
 *   object.
 * </p>
 * 
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
public class AbstractSelectControl<Ttarget,Tvalue>
  extends ControlGroup<Ttarget>
{
  
  private String name;
  private StringConverter<Ttarget> converter;
  private Channel<?> source;
  private Expression<?> sourceExpression;
  protected boolean multi=false;
  private VariableMapBinding<Ttarget> binding;
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setSource(Expression<?> sourceExpression)
  { this.sourceExpression=sourceExpression;
  }


  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public Channel<Ttarget> bindTarget(Focus<?> parentFocus)
    throws ContextualException
  { 
    Channel<Ttarget> target=(Channel<Ttarget>) super.bindTarget(parentFocus);
    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
    
    if (sourceExpression!=null)
    { source=parentFocus.bind(sourceExpression);
    }
    return target;
  }
  
  @Override
  protected Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  { 
    binding=new VariableMapBinding<Ttarget>(valueBinding,null,converter);
    return focus.chain(source);
  }
  
  
  @Override
  public String getVariableName()
  { return name;
  }
  

  
//  void setValueConverter(StringConverter<Tvalue> converter)
//  { this.valueConverter=converter;
//  }
  
  @Override
  public void gather(ServiceContext context)
  {
    SelectState<Ttarget,Tvalue> state=getState(context);
    if (context.getPost()==null)
    { return;
    }

    Ttarget val;
    List<String> strings=context.getPost().get(state.getVariableName());
    if (debug)
    { log.fine("Read ["+strings+"] from posted formvar "+state.getVariableName());
    }
    

    try
    {
      if (strings==null || strings.size()==0)
      { 
        if (!state.getPresented())
        {
          if (debug)
          { 
            log.fine
              (getLogPrefix()
              +"Ignoring missing value for not-presented control"
              );
          }
          return;
        }
      
        val=null;
      }
      else
      {

        val=binding.convertInput(strings);
        if (debug)
        { log.fine("Got selection ["+val+"] for "+getVariableName());
        }
        
      }

      state.setValue(val);
      if (conditionallyUpdateTarget(val,state.getPreviousValue()))
      { state.valueUpdated();
      }   
        
    }
    catch (AccessException x)
    { handleException(context,x);
    }
    catch (NumberFormatException x)
    { handleException(context,x);
    }

  }
  
  @Override
  public String toString()
  { return super.toString()+": name="+name;
  }
  
  
  @Override
  public void scatter(ServiceContext context)
  {
    SelectState<Ttarget,Tvalue> state=getState(context);
    if (target!=null)
    {
      try
      {
        state.updateValue(target.get());
        
      }
      catch (AccessException x)
      { handleException(context,x);
      }
      catch (NumberFormatException x)
      { handleException(context,x);
      }

      
    }
  }
  
  
  
  @Override
  public SelectState<Ttarget,Tvalue> createState()
  { return new SelectState<Ttarget,Tvalue>(this);
  }  
  
  @SuppressWarnings("unchecked")
  @Override
  protected SelectState<Ttarget,Tvalue> getState(Dispatcher context)
  { return (SelectState<Ttarget,Tvalue>) context.getState();
  }
}



