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




import spiralcraft.data.session.Buffer;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.textgen.EventContext;

public class TupleEditor
    extends spiralcraft.servlet.webui.components.TupleEditor
{

  private final AbstractTag tag=new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "DIV";
    }
    
    protected boolean hasContent()
    { return true;
    }
    
    protected void renderContent(EventContext context)
      throws IOException
    { TupleEditor.super.render(context);
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    { super.renderAttributes(context);
    }
  };
  
  private ErrorTag errorTag
    =new ErrorTag(tag);

  public AbstractTag getTag()
  { return tag;
  }
  
  public AbstractTag getErrorTag()
  { return errorTag;
  }

  public void render(EventContext context)
    throws IOException
  { 
    if ( ((ControlState<?>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }

  protected Focus<Buffer> bindExports()
    throws BindException
  {
    if (findElement(Form.class)==null)
    { throw new BindException("Editor must be contained in a Form");
    }
    return super.bindExports();
  }
  

}
