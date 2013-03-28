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


import spiralcraft.app.Dispatcher;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLog;

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
  private static final ClassLog log
    =ClassLog.getInstance(TextInput.class);
  
  private String name;
  private boolean reverse;
  
  
  private class Tag
    extends AbstractTag
  {
    { addStandardClass("sc-webui-checkbox-input sc-webui-checkbox");
    }
    
    @Override
    protected String getTagName(Dispatcher context)
    { return "input";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      ControlState<Boolean> state=getState(context);
      renderAttribute(out,"type","checkbox");
      renderAttribute(out,"name",state.getVariableName());
      renderAttribute(out,"value","on");
      Boolean val=state.getValue();
      if (val!=null && val)
      { renderAttribute(out,"checked","checked");
      }
      super.renderAttributes(context,out);
    }
    
    @Override
    protected boolean hasContent()
    { return false;
    }
    
  };
  
  private Tag tag=new Tag();
  private ErrorTag errorTag=new ErrorTag();
  
  @Override
  protected void addHandlers()
    throws ContextualException
  { 
    addHandler(errorTag);
    addHandler(tag);
    
    FormField<?> formField=this.findComponent(FormField.class);
    if (formField!=null)
    { 
      addHandler(formField.newInputHandler());
      tag.setGenerateId(true);
    }
    
    super.addHandlers();
  }
  
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
  
  public Tag getTag()
  { return tag;
  }

  public ErrorTag getErrorTag()
  { return errorTag;
  }

  @Override
  public Focus<?> bindStandard(Focus<?> focus)
    throws ContextualException
  { 
    super.bindStandard(focus);
    
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
    return focus;
        
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
  public void gather(ServiceContext context)
  {
    ControlState<Boolean> state=getState(context);
    
    // Only update if changed
    if (context.getPost()!=null)
    {
      String post=context.getPost().getFirst(state.getVariableName());

      Boolean value=post!=null && post.equals("on");
      Boolean previousValue=Boolean.TRUE.equals(state.getPreviousValue());
      
      state.setValue(value);
      
      try
      { 
        if (!reverse)
        { 
          if (conditionallyUpdateTarget(value,previousValue))
          { state.valueUpdated();
          }
        }
        else
        { 
          if (conditionallyUpdateTarget(!value,!previousValue))
          { state.valueUpdated();
          }
        }
      }
      catch (AccessException x)
      { handleException(context,x);
      }
    }

  }
  
  
  @Override
  public void scatter(ServiceContext context)
  {
    ControlState<Boolean> state=getState(context);
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
        state.updateValue(val);
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

