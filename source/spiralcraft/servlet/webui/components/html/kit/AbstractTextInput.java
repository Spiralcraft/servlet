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
package spiralcraft.servlet.webui.components.html.kit;

import java.io.IOException;



import spiralcraft.app.Dispatcher;


import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.AbstractTextControl;
import spiralcraft.servlet.webui.components.html.AbstractTag;
import spiralcraft.servlet.webui.components.html.ErrorTag;
import spiralcraft.servlet.webui.components.html.PeerJSTag;
import spiralcraft.text.MessageFormat;

public abstract class AbstractTextInput<Ttarget>
  extends AbstractTextControl<Ttarget>
{

  
  private TextTag tag=createTag();
  
  public class TextTag 
    extends AbstractTag
  {
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "input";
    }

    public void setSize(int size)
    { this.appendAttribute("size",Integer.toString(size));
    }

    public void setMaxlength(int maxlength)
    { this.appendAttribute("maxlength",Integer.toString(maxlength));
    }
 
    public void setPlaceholder(MessageFormat val)
    { this.addStandardBinding("placeholder",val);
    }
    
    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      ControlState<String> state=getState(context);
      renderAttribute(out,"type",getInputType());
      renderAttribute(out,"name",state.getVariableName());
      if (shouldRenderValue() && state.getValue()!=null)
      { renderAttribute(out,"value",state.getValue());
      }

      state.setPresented(true);
      super.renderAttributes(context,out);
    }
    
    @Override
    protected boolean hasContent()
    { return false;
    }
    
    protected String getInputType()
    { return "text";
    }
    
    protected boolean shouldRenderValue()
    { return true;
    }
    
  }
  
  private ErrorTag errorTag=new ErrorTag();
  
  
  { 
    addHandler(errorTag);
    addHandler(tag);
  }
  
  public TextTag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  public void setPeerJSTag(PeerJSTag scriptTag)
  { 
    tag.setGenerateId(true);
    tag.setTagPosition(-1);
    scriptTag.setTagPosition(1);
    addHandler(scriptTag);
    
  }
  
  protected abstract TextTag createTag();

}

