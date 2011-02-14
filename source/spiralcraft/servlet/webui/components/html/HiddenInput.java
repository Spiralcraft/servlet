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



import spiralcraft.textgen.EventContext;

//import spiralcraft.log.ClassLog;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.AbstractTextControl;

public class HiddenInput<Ttarget>
  extends AbstractTextControl<Ttarget>
{
//  private static final ClassLog log
//    =ClassLog.getInstance(TextInput.class);
  
  
  private Tag tag=new Tag();
  protected boolean password;
  
  class Tag 
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return "input";
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<Ttarget> state=getState(context);
      renderAttribute(context.getOutput(),"type","hidden");
      renderAttribute(context.getOutput(),"name",state.getVariableName());
      if (state.getValue()!=null && !password)
      { renderAttribute(context.getOutput(),"value",(String) state.getValue());
      }
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return false;
    }
    
  }
  
  private ErrorTag errorTag=new ErrorTag(tag);
  

  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }

  /**
   * Password mode will not render output
   * 
   * @param password
   * @return
   */
  public void setPassword(boolean password)
  { this.password=password;
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    ControlState<?> state=getState(context);
    if (state.isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
    super.render(context);
    state.setPresented(true);
    
  }
  

  @Override
  public Focus<?> bindSelf(Focus<?> focus)
    throws BindException
  { 
    focus=super.bindSelf(focus);
    tag.bind(focus);
    errorTag.bind(focus);
    return focus;
  }    
  


}

