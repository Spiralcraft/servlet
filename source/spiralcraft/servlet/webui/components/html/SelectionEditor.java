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

import spiralcraft.common.ContextualException;
import spiralcraft.data.DataComposite;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.app.Dispatcher;

public class SelectionEditor<TorigContent extends DataComposite,TselectItem>
    extends spiralcraft.servlet.webui.components.SelectionEditor<TorigContent,TselectItem>
{

  public class Tag
    extends AbstractTag
  {
    private String tagName;
    
    public void setTagName(String tagName)
    { this.tagName=tagName;
    }
    
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return tagName;
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    

  }
  
  private Tag tag=new Tag();
  
  private ErrorTag errorTag
    =new ErrorTag();
  
  
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


  @Override
  protected Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  {
    if (findComponent(Form.class)==null)
    { throw new BindException("Editor must be contained in a Form");
    }
    focus=super.bindExports(focus);
    return focus;
  }

}
