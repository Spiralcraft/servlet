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

import spiralcraft.textgen.EventContext;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.AbstractCommandControl;
import spiralcraft.servlet.webui.components.CommandState;

import spiralcraft.lang.BindException;
import spiralcraft.net.http.VariableMap;

/**
 * <P>A Button control, bound to a Command. The "x" (binding target)
 *   property contains an expression that resolves an instance of a Command to 
 *   execute. Note that if the "type" property is not set to "submit" (the
 *   default value), the Button will not trigger a post to the server, and
 *   the command will not execute.
 * </P>
 * 
 * <P>&lt;INPUT type="<i>submit</i>"&gt;
 * </P>
 *  
 * @author mike
 *
 */
public class Button<Tcontext,Tresult>
  extends AbstractCommandControl<Tcontext,Tresult>
{

  private String name;
  private String value="submit";
  private String type="submit";
  
  private Tag tag=new Tag();
  
  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return "button";
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      CommandState<Tcontext,Tresult> state=getState(context);
      renderAttribute(context.getWriter(),"type",type);
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      renderAttribute(context.getWriter(),"value",value);
      super.renderAttributes(context);
    }

    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { Button.super.render(context);
    }
  }
    
  private ErrorTag errorTag=new ErrorTag(tag);
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setValue(String value)
  { this.value=value;
  }
  
  public void setType(String type)
  { this.type=type;
  }


  @Override
  public String getVariableName()
  { return name;
  }
  

  @Override
  public void render(EventContext context)
    throws IOException
  { 
    pushState(context);
    try
    {
      if (getState(context).isErrorState())
      { errorTag.render(context);
      }
      else
      { tag.render(context);
      }
    }
    finally
    { popState(context);
    }
  }
  
  @Override
  public void gather(ServiceContext context)
  {
    CommandState<Tcontext,Tresult> state=getState(context);
    VariableMap post=context.getPost();
    boolean gotPost=false;
    if (post!=null)
    { gotPost=post.getOne(state.getVariableName())!=null;
    }

    if (gotPost)
    { executeCommand(context);
    }
    
    if (debug)
    { 
      log.fine
        ("Button: readPost- "+state.getVariableName()+"="
            +context.getPost().getOne(state.getVariableName())
        );
    }
  }
  
  @Override
  public void scatter(ServiceContext context)
  { 
  }

  @Override
  public void bindSelf()
    throws BindException
  { 
    tag.bind(getFocus());
    errorTag.bind(getFocus());
  }  

}

