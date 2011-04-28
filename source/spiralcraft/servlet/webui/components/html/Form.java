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

// import spiralcraft.log.ClassLog;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.Acceptor;




import java.io.IOException;

/**
 * An HTML form
 * 
 * @author mike
 *
 * @param <T>
 */
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
  	private String name;
  	
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return tagName;
    }
    
    public void setTagName(String tagName)
    { this.tagName=tagName;
    }
    
    public void setName(String name)
    { this.name=name;
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    { 
      
      String actionURI
        =((ServiceContext) context)
          .registerAction(createAction(context,true));
      
      renderPresentAttribute(out,"name",name);
      renderAttribute(out,"action",actionURI);
      renderAttribute(out,"method","post");
      if (mimeEncoded)
      { renderAttribute(out,"enctype","multipart/form-data");
      }
      super.renderAttributes(context,out);
    }
  }
  
  private final ErrorTag errorTag=new ErrorTag();
  
  
  { 
    addHandler(errorTag);
    addHandler(tag);
  }
  
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

}
