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

//import spiralcraft.log.ClassLog;

import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.AbstractTextControl;

public class TextInput<Ttarget>
  extends AbstractTextControl<Ttarget>
{
//  private static final ClassLog log
//    =ClassLog.getInstance(TextInput.class);
  
  private boolean password;
  
  
  /**
   * Whether the control is in password mode
   * 
   * @param password
   */
  public void setPassword(boolean password)
  { this.password=password;
  }
  
  private Tag tag=new Tag();
  
  public class Tag 
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
    
    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      ControlState<String> state=getState(context);
      if (password)
      { renderAttribute(out,"type","password");
      }
      else
      { renderAttribute(out,"type","text");
      }
      renderAttribute(out,"name",state.getVariableName());
      if (!password && state.getValue()!=null)
      { renderAttribute(out,"value",state.getValue());
      }

      state.setPresented(true);
      super.renderAttributes(context,out);
    }
    
    @Override
    protected boolean hasContent()
    { return false;
    }
    
  }
  
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

}

