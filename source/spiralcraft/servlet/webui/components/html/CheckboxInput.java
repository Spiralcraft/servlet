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

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

/**
 * <P>A Checkbox used to represent a boolean value, bindable to boolean Channel
 * </P>
 * 
 * <P>&lt;INPUT type="checkbox"&gt;
 * </P>
 * 
 * @author mike
 *
 */
public class CheckboxInput
  extends Control<Boolean>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(TextInput.class);
  
  private String name;
  private boolean reverse;
  
  
  private AbstractTag tag
    =new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "input";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<Boolean> state=((ControlState<Boolean>) context.getState());
      renderAttribute(context.getWriter(),"type","checkbox");
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      renderAttribute(context.getWriter(),"value","on");
      Boolean val=state.getValue();
      if (val!=null && val)
      { renderAttribute(context.getWriter(),"checked","checked");
      }
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return false;
    }
    
  };
  
  private ErrorTag errorTag=new ErrorTag(tag);
  
  public void setName(String name)
  { this.name=name;
  }
  
  /**
   * <p>Indicate that, when checked, a value of false should be written to
   *   the bound target
   * </p>
   * 
   * @param reverse
   */
  public void setReverse(boolean reverse)
  { this.reverse=reverse;
  }
  
  public AbstractTag getTag()
  { return tag;
  }


  @Override
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    super.bind(childUnits);
    
    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
    else if 
      (!Boolean.TYPE.isAssignableFrom(target.getContentType())
       && !Boolean.class.isAssignableFrom(target.getContentType())
       )
    { 
      throw new BindException
        ("CheckboxInput can only be bound to a boolean target");
    }
        
  }
  
  @Override
  public String getVariableName()
  { return name;
  }
  
  @Override
  public ControlState<Boolean> createState()
  { return new ControlState<Boolean>(this);
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    if ( ((ControlState<?>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
    super.render(context);
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    ControlState<Boolean> state=((ControlState<Boolean>) context.getState());
    
    // Only update if changed
    if (context.getPost()!=null)
    {
      String post=context.getPost().getOne(state.getVariableName());
      boolean value=post!=null && post.equals("on");
      
      if (state.updateValue(value))
      {
    
        if (target!=null)
        {
        
          try
          { 
            if (!reverse)
            { target.set(value);
            }
            else
            { target.set(!value);
            }
          }
          catch (AccessException x)
          { state.setException(x);
          }
        }

      }
    }

  }
  
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    ControlState<Boolean> state=((ControlState<Boolean>) context.getState());
    if (target!=null)
    {
      try
      {
        Boolean val=target.get();
        if (reverse && val!=null)
        { val=!val;
        }
        
        if (debug)
        { log.fine(toString()+" scattering "+val);
        }
        state.setValue(val);
      }
      catch (AccessException x)
      { state.setException(x);
      }
      catch (NumberFormatException x)
      { state.setException(x);
      }

      
    }
  }

}

