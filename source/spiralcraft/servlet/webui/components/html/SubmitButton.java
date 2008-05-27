package spiralcraft.servlet.webui.components.html;

import java.io.IOException;

import spiralcraft.textgen.EventContext;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.command.Command;
import spiralcraft.lang.AccessException;
import spiralcraft.net.http.VariableMap;

public class SubmitButton
  extends Control<Command<?,?>>
{

  private String name;
  private String label;
  
  private Tag tag=new Tag();
  
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
      renderAttribute(context.getWriter(),"type","submit");
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      
      // Yes, we are renaming it
      renderAttribute(context.getWriter(),"value",label);
      super.renderAttributes(context);
    }

    @Override
    protected boolean hasContent()
    { return false;
    }
  };
    
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
  
  public void setLabel(String label)
  { this.label=label;
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
    { gotPost=post.getOne(state.getVariableName())!=null;
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


}

