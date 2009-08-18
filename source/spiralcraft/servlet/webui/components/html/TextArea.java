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
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.AbstractTextControl;

public class TextArea<Ttarget>
  extends AbstractTextControl<Ttarget>
{
//  private static final ClassLog log
//    =ClassLog.getInstance(TextInput.class);
  
  
  private Tag tag=new Tag();
  
  private ErrorTag errorTag=new ErrorTag(tag);
  
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
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
    ((ControlState<?>) context.getState()).setPresented(true);
  }  

  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return "textarea";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<String> state=((ControlState<String>) context.getState());
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { 
      String value=((ControlState<String>) context.getState()).getValue();
      if (value!=null)
      {  context.getWriter().write(value);
      }
    }

    public void setRows(String val)
    { appendAttribute("rows",val);
    }
  
    public void setCols(String val)
    { appendAttribute("cols",val);
    }
  
    public void setDisabled(String val)
    { appendAttribute("disabled",val);
    }
  
    public void setName(String val)
    { appendAttribute("name",val);
    }
  
    public void setReadOnly(String val)
    { appendAttribute("readonly",val);
    }
    
    
  }
  
  @Override
  public void bindSelf()
    throws BindException
  { 
    tag.bind(getFocus());
    errorTag.bind(getFocus());
  }       
  
}

