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
import spiralcraft.app.MessageHandlerChain;

//import spiralcraft.log.ClassLog;

import spiralcraft.app.Message;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.kit.AbstractTextControl;
import spiralcraft.text.MessageFormat;
import spiralcraft.text.html.TextAreaEncoder;
import spiralcraft.textgen.OutputContext;

public class TextArea<Ttarget>
  extends AbstractTextControl<Ttarget>
{
//  private static final ClassLog log
//    =ClassLog.getInstance(TextInput.class);
  
  private Tag tag=new Tag();  
  private ErrorTag errorTag=new ErrorTag();
    
  
  { 
    addHandler(errorTag);
    addHandler(tag);
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  public class Tag
    extends AbstractTag
  {
    { addStandardClass("sc-webui-text-area");
    }
    
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "textarea";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      ControlState<?> state=getState(context);
      renderAttribute(out,"name",state.getVariableName());
      state.setPresented(true);
      super.renderAttributes(context,out);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
    protected void renderContent
      (Dispatcher context
      ,Message message
      ,MessageHandlerChain next
      )
      throws IOException
    { 
      String value=TextArea.this.<String>getState(context).getValue();
      if (value!=null)
      { OutputContext.get().append(TextAreaEncoder.encode(value));
      }
      super.renderContent(context,message,next);
      
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
    
    public void setPlaceholder(MessageFormat val)
    { this.addStandardBinding("placeholder",val);
    }
    
  }  
  
}

