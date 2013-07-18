//
//Copyright (c) 2013 Michael Toth
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
import spiralcraft.servlet.webui.Component;

public class Label
  extends Component
{
  

  private Tag tag=new Tag();
  private FormField<?> formField;

  @Override
  protected void addHandlers()
    throws ContextualException
  { 
    addHandler(tag);
    
    formField=this.findComponent(FormField.class);
    super.addHandlers();
  }  
  
  public Tag getTag()
  { return tag;
  }
  
  public class Tag extends AbstractTag
  {
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "label";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    { 
      if (formField!=null)
      { 
        String inputId=formField.getInputId();
        if (inputId!=null)
        { renderAttribute(out,"for",formField.getInputId());
        }
      }
      super.renderAttributes(context,out);
    }

    @Override
    protected boolean hasContent()
    { return getChildCount()>0;
    }
    
    
  }
  
}