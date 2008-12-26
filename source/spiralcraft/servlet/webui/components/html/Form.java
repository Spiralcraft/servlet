//
//Copyright (c) 1998,2007 Michael Toth
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

import spiralcraft.textgen.EventContext;

// import spiralcraft.log.ClassLog;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.components.Acceptor;




import java.io.IOException;

public class Form<T>
  extends Acceptor<T>
{
  // private static final ClassLog log=ClassLog.getInstance(Form.class);  
  
  private boolean mimeEncoded;
  
  private final Tag tag=new Tag();
  
  class Tag
    extends AbstractTag
  {
  	private String tagName="form";
  	
    @Override
    protected String getTagName(EventContext context)
    { return tagName;
    }
    
    public void setTagName(String tagName)
    { this.tagName=tagName;
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { Form.super.render(context);
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    { 
      
      String actionURI
        =((ServiceContext) context)
          .registerAction(createAction(context,true));
      
      
      renderAttribute(context.getWriter(),"action",actionURI);
      renderAttribute(context.getWriter(),"method","post");
      if (mimeEncoded)
      { renderAttribute(context.getWriter(),"enctype","multipart/form-data");
      }
      super.renderAttributes(context);
    }
  }
  
  private final ErrorTag errorTag=new ErrorTag(tag);
  
  /**
   * 
   * @return The Tag which controls the HTML markup output of this Control
   */
  public Tag getTag()
  { return tag;
  }
  
  /**
   * 
   * @return The ErrorTag which outputs error information associated with
   *   this Control
   */
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  @Override
  protected boolean wasActioned(ServiceContext context)
  { return context.getPost()!=null;
  }

  
  public void setMimeEncoded(boolean mimeEncoded)
  { this.mimeEncoded=mimeEncoded;
  }
  
  public void renderError(ServiceContext context)
    throws IOException
  { new ErrorTag(tag).render(context);
  }  
  
  @SuppressWarnings("unchecked")
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    ControlGroupState<T> state=((ControlGroupState<T>) context.getState());
    if (state.isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }

  
  

  

}
