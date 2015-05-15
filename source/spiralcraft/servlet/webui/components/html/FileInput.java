//
//Copyright (c) 1998,2015 Michael Toth
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
import java.net.URI;

import spiralcraft.app.Dispatcher;
import spiralcraft.common.ContextualException;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.kit.AbstractFileControl;

/**
 * Accepts a file upload and records the relative URI of the uploaded file
 * 
 * @author mike
 *
 */
public class FileInput
  extends AbstractFileControl
{

  
  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "input";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      ControlState<URI> state=getState(context);
      renderAttribute(out,"type","file");
      renderAttribute(out,"name",state.getVariableName());
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
    super.addHandlers();
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

