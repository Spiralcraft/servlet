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

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.ActionControlState;
import spiralcraft.servlet.webui.components.html.kit.AbstractHtmlActionControl;

import spiralcraft.net.http.VariableMap;

/**
 * <P>An Image based submit button, bound to a Command. The "x" (binding target)
 *   property contains an
 *   expression that resolves an instance of a Command to execute.
 * </P>
 * 
 * <P>&lt;INPUT type="<i>image</i>" alt="<i>sometext</i>" 
 *  src="<i>imageURI</i>"&gt;
 * </P>
 *  
 * @author mike
 *
 */
public class ImageButton<Tcontext,Tresult>
  extends AbstractHtmlActionControl<Tcontext,Tresult>
{

  private String src;
  private String alt;
  
  { 
    tag
      =new Tag();
  }
  
  
  
  @SuppressWarnings("unchecked")
  public Tag getTag()
  { return (Tag) tag;
  }

  
  public void setSrc(String src)
  { this.src=src;
  }
  
  public void setAlt(String alt)
  { this.alt=alt;
  }
  
  @Override
  public void gather(ServiceContext context)
  {
    ActionControlState<Tcontext,Tresult> state=getState(context);
    VariableMap post=context.getForm();
    boolean gotPost=false;
    if (post!=null)
    { 
      gotPost=post.getFirst(state.getVariableName()+".x")!=null;
      if (debug)
      { log.fine(toString()+(gotPost?": got pressed":": didn't get pressed")); 
      }
    }

    if (gotPost)
    { fireAction(context);
    }
    
    
    if (debug)
    { 
      log.fine
        ("ImageButton: readPost- "+state.getVariableName()+"="
            +context.getForm().getFirst(state.getVariableName())
        );
    }
  }
  

  public class Tag
    extends AbstractTag
  {
    { addStandardClass("sc-webui-image-button");
    }
    
    private String height;
    private String width;
    private String border;
    private String tagAlt;
    
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "input";
    }

    public void setHeight(String height)
    { this.height=height;
    }
    
    public void setWidth(String width)
    { this.width=width;
    }
    
    public void setBorder(String border)
    { this.border=border;
    }
    
    public void setAlt(String alt)
    { this.tagAlt=alt;
    }
    
    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      ActionControlState<Tcontext,Tresult> state=getState(context);
      renderAttribute(out,"type","image");
      renderPresentAttribute(out,"src",src);
      renderPresentAttribute(out,"alt",ImageButton.this.alt);
      renderPresentAttribute(out,"alt",tagAlt);
      renderPresentAttribute(out,"height",height);
      renderPresentAttribute(out,"width",width);
      renderPresentAttribute(out,"border",border);
      renderAttribute(out,"name",state.getVariableName());
      super.renderAttributes(context,out);
    }

    @Override
    protected boolean hasContent()
    { return false;
    }
  }


  @Override
  protected void scatter(ServiceContext context)
  {
    // TODO Auto-generated method stub
    
  }
  
}

