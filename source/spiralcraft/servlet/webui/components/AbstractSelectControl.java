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


import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;

import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ServiceContext;

/**
 * <P>A standard HTML SELECT list, bound to a target and a source.
 * </P>
 * 
 * <P>The "x" (binding target) property contains an expression that references
 *   the currently selected item(s). The optional "source" property contains an 
 *   expression that provides a list of candidate values, used for generating 
 *   the set of options (see the Option class).
 * </P>
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
//  private StringConverter<Tvalue> valueConverter;
  private Channel<?> source;
  private Expression<?> sourceExpression;
  protected boolean multi=false;
  
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setSource(Expression<?> sourceExpression)
  { this.sourceExpression=sourceExpression;
  }


  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public Channel<Ttarget> bindTarget(Focus<?> parentFocus)
    throws BindException
  { 
    Channel<Ttarget> target=(Channel<Ttarget>) super.bindTarget(parentFocus);
    if (converter==null && target!=null)
    { 
      converter=target.getReflector().getStringConverter();
    }
    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
    
    if (sourceExpression!=null)
    { source=parentFocus.bind(sourceExpression);
    }
    return target;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public Focus<?> bindExports()
    throws BindException
  { 
    Focus<?> focus=new SimpleFocus(getFocus(),source);
    return focus; 
  }
  
  
  @Override
  public String getVariableName()
  { return name;
  }
  

  
//  void setValueConverter(StringConverter<Tvalue> converter)
//  { this.valueConverter=converter;
//  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    SelectState<Ttarget,Tvalue> state
      =((SelectState<Ttarget,Tvalue>) context.getState());
    
    if (multi)
    {
      log.fine("Multiselect not implemented");
    }
    else
    {
      Ttarget val;
      try
      {
        if (context.getPost()==null)
        { return;
        }
        
        List<String> strings=context.getPost().get(state.getVariableName());
        if (debug)
        { log.fine("Read ["+strings+"] from posted formvar "+state.getVariableName());
        }
        
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
        else if (strings.get(0)!=null && !(strings.get(0).length()==0))
        { 
          if (converter!=null)
          { val=converter.fromString(strings.get(0));
          }
          else
          { val=(Ttarget) strings.get(0);
          }
        }
        else
        { val=null;
        }
        if (debug)
        { log.fine("Got selection ["+val+"] for "+getVariableName());
        }

        
        if (state.updateValue(val))
        { conditionallyUpdateTarget(val);
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
  
  @Override
  public String toString()
  { return super.toString()+": name="+name;
  }
  
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    SelectState<Ttarget,Tvalue> state=
      ((SelectState<Ttarget,Tvalue>) context.getState());
    if (target!=null)
    {
      try
      {
        state.setValue(target.get());
        
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
}



