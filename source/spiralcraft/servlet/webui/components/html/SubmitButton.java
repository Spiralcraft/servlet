package spiralcraft.servlet.webui.components.html;

import java.io.IOException;

import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Element;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.lang.AccessException;

public class SubmitButton
  extends Control<Boolean>
{

  private String name;
  private String label;
  
  private AbstractTag tag=new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "input";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<Boolean> state=((ControlState<Boolean>) context.getState());
      renderAttribute(context.getWriter(),"type","submit");
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      renderAttribute(context.getWriter(),"value",label);
    }

    @Override
    protected boolean hasContent()
    { return false;
    }
  };
  
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setLabel(String label)
  { this.label=label;
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
  { tag.render(context);
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    ControlState<Boolean> state=((ControlState<Boolean>) context.getState());
    if (state.updateValue
          (context.getPost().getOne(state.getVariableName())!=null)
       )
    {
      if (target!=null)
      { 
        try
        { target.set(state.getValue());
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
  }
}

