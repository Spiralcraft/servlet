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
import java.io.Writer;

import spiralcraft.textgen.EventContext;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.command.Command;
import spiralcraft.lang.AccessException;
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
public class ImageButton
  extends Control<Command<?,?>>
{

  private String name;
  private String src;
  private String alt;
  
  private Tag tag
    =new Tag();
  
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
  
  public void setSrc(String src)
  { this.src=src;
  }
  
  public void setAlt(String alt)
  { this.alt=alt;
  }
  

  @Override
  public String getVariableName()
  { return name;
  }
  
  @Override
  public ControlState<Command<?,?>> createState()
  { return new ControlState<Command<?,?>>(this);
  }

  @Override
  public void render(EventContext context)
    throws IOException
  { 
    if (((ControlState<?>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
    super.render(context);
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    ControlState<Command> state=((ControlState<Command>) context.getState());
    VariableMap post=context.getPost();
    boolean gotPost=false;
    if (post!=null)
    { 
      gotPost=post.getOne(state.getVariableName()+".x")!=null;
      if (debug)
      { log.fine(toString()+(gotPost?": got pressed":": didn't get pressed")); 
      }
    }

    if (gotPost)
    {
      if (target!=null)
      { 
        try
        { 
          Command command=state.getValue();
          if (command==null)
          { 
            // Might be a default binding
            Object oCommand=target.get();
            if (oCommand instanceof Command)
            { command=(Command) oCommand;
            }
          }
          
          if (command!=null)
          {
            // Queueing should be decided by the Command, which should
            //   interact with WebUI api to coordinate- controller role.
            command.execute();
            if (command.getException()!=null)
            { handleException(context,command.getException());
            }
          }
        }
        catch (AccessException x)
        { handleException(context,x);
        }
      }
    }
    
    
    if (debug)
    { 
      log.fine
        ("ImageButton: readPost- "+state.getVariableName()+"="
            +context.getPost().getOne(state.getVariableName())
        );
    }
  }
  

  public class Tag
    extends AbstractTag
  {
    private String height;
    private String width;
    private String border;
    private String alt;
    
    @Override
    protected String getTagName(EventContext context)
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
    { this.alt=alt;
    }
    
    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<Command> state=((ControlState<Command>) context.getState());
      Writer writer=context.getWriter();
      renderAttribute(writer,"type","image");
      renderPresentAttribute(writer,"src",src);
      renderPresentAttribute(writer,"alt",ImageButton.this.alt);
      renderPresentAttribute(writer,"alt",alt);
      renderPresentAttribute(writer,"height",height);
      renderPresentAttribute(writer,"width",width);
      renderPresentAttribute(writer,"border",border);
      renderAttribute(writer,"name",state.getVariableName());
      super.renderAttributes(context);
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

