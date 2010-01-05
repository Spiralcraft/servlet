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




import spiralcraft.data.DataComposite;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.textgen.EventContext;

/**
 * HTML extension for editing an Aggregate (a collection of DataComposites)  
 * 
 * @author mike
 *
 * @param <T>
 */
public class AggregateEditor<T extends DataComposite>
    extends spiralcraft.servlet.webui.components.AggregateEditor<T>
{

  public class Tag
    extends AbstractTag
  {
    private String tagName;
    
    public void setTagName(String tagName)
    { this.tagName=tagName;
    }
    
    @Override
    protected String getTagName(EventContext context)
    { return tagName;
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { AggregateEditor.super.render(context);
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    { super.renderAttributes(context);
    }
  }
  
  private Tag tag=new Tag();
  
  private ErrorTag errorTag
    =new ErrorTag(tag);

  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }

  @Override
  public Focus<?> bindExports()
    throws BindException
  { 
    Focus<?> focus=super.bindExports();
    tag.bind(getFocus());
    errorTag.bind(getFocus());
    return focus;
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    if ( getState(context).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }


  

}
