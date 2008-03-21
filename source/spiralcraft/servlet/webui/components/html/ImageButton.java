package spiralcraft.servlet.webui.components.html;

import java.io.IOException;

import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Element;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.command.Command;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Expression;
import spiralcraft.net.http.VariableMap;

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
  public void setParent(Element parentElement)
    throws MarkupException
  { 
    super.setParent(parentElement);
    // controlGroup=parentElement.findElement(ControlGroup.class);
    // controlGroup.registerControl(getPath(),this);
  }

  public String getVariableName()
  { return name;
  }
  
  @Override
  public ControlState<Boolean> createState()
  { return new ControlState<Boolean>(this);
  }

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
            { state.setException(command.getException());
            }
          }
        }
        catch (AccessException x)
        { state.setError(x.getMessage());
        }
      }
    }
    
    
//    System.err.println
//      ("SubmitButton: readPost- "
//      +context.getPost().getOne(state.getVariableName())
//      );
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  { 
    ControlState<Boolean> state=((ControlState<Boolean>) context.getState());
    state.setError(null);
    // At some point we need to read a command
  }

  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return "INPUT";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<Command> state=((ControlState<Command>) context.getState());
      renderAttribute(context.getWriter(),"type","image");
      renderAttribute(context.getWriter(),"src",src);
      renderAttribute(context.getWriter(),"alt",alt);
      renderAttribute(context.getWriter(),"name",state.getVariableName());
    }

    @Override
    protected boolean hasContent()
    { return false;
    }
  }
  
}

