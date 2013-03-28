//
//Copyright (c) 1998,2011 Michael Toth
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

import spiralcraft.app.Dispatcher;
import spiralcraft.common.ContextualException;

import spiralcraft.servlet.webui.components.html.kit.HtmlContainer;

/**
 * <p>An arbitrary container that has a server-side representation
 * </p>
 *  
 * @author mike
 *
 */
public class FieldSet
  extends HtmlContainer
{
  
  
  private Tag tag=new Tag();
  private ErrorTag errorTag=new ErrorTag();
  
  public class Tag extends AbstractTag
  {

    
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "fieldset";
    }

    @Override
    protected boolean hasContent()
    { return true;
    }
    
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }

  @Override
  public void addHandlers()
    throws ContextualException
  { 
    super.addHandlers();
    addHandler(errorTag);
    addHandler(tag);
    FormField<?> formField=this.findComponent(FormField.class);
    if (formField!=null)
    { 
      addHandler(formField.newInputHandler());
      tag.setGenerateId(true);
    }

  }
  
  
  
}
