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
package spiralcraft.servlet.webui.components.html;

import java.io.IOException;
import java.util.List;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.util.StringConverter;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

public class Option<Ttarget,Tvalue>
  extends Control<Ttarget>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(TextInput.class);
  
  private StringConverter<Tvalue> converter;
  private Expression<Tvalue> valueExpression;
  private Channel<Tvalue> value;
  
  private AbstractTag tag
    =new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "OPTION";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      OptionState<Tvalue> state=((OptionState<Tvalue>) context.getState());

      if (state.isSelected())
      { renderAttribute(context.getWriter(),"selected",null);
      }
      if (converter!=null)
      { 
        renderAttribute
          (context.getWriter(),"value",converter.toString(state.getValue())
          );
      }
      else
      {
        if (state.getValue()!=null)
        {
          renderAttribute
            (context.getWriter()
            ,"value"
            ,state.getValue()!=null
            ?state.getValue().toString()
            :""
            );
        }
      }
      
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { Option.super.render(context);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
  };
  
  private ErrorTag errorTag=new ErrorTag(tag);
  
  public void setValue(Expression<Tvalue> valueExpression)
  { this.valueExpression=valueExpression;
  }
  

  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    super.bind(childUnits);
    value=getFocus().bind(valueExpression);
    if (converter==null && value!=null)
    { 
      converter=
        (StringConverter) 
        StringConverter.getInstance(value.getContentType());
    }
    if (target==null)
    { log.fine("Not bound to anything (Option)");
    }
  }
  
  public String getVariableName()
  { return null;
  }
  
  @Override
  public OptionState<Tvalue> createState()
  { return new OptionState<Tvalue>(this);
  }
  
  @SuppressWarnings("unchecked")
  public void render(EventContext context)
    throws IOException
  { 
    if (((OptionState<Tvalue>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    // SelectList does all the gathering here.
  }
  
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    OptionState<Tvalue> state=((OptionState<Tvalue>) context.getState());
    if (value!=null)
    {
      try
      { 
        state.setValue(value.get());
        state.setSelected(state.getSelectState().isSelected(state.getValue()));
      }
      catch (AccessException x)
      { 
        state.setError(x.getMessage());
        state.setException(x);
      }
      catch (NumberFormatException x)
      { 
        state.setError(x.getMessage());
        state.setException(x);
      }

      
    }
    
    
  }
  

  
}

class OptionState<Tvalue>
  extends ControlState<Tvalue>
{
  private boolean selected=false;
  
  public OptionState(Option<?,?> control)
  { super(control);
  }
  
  public boolean isSelected()
  { return selected;
  }
  
  public void setSelected(boolean selected)
  { this.selected=selected;
  }
  
  public SelectState<?,Tvalue> getSelectState()
  { return (SelectState<?,Tvalue>) controlGroupState;
  }
  
  
  
}

