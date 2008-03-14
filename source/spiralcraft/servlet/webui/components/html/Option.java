package spiralcraft.servlet.webui.components.html;

import java.io.IOException;
import java.util.List;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.util.StringConverter;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Element;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

public class Option<Ttarget,Tvalue>
  extends Control<Ttarget>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(TextInput.class);
  
  private StringConverter<Tvalue> converter;
  private Expression<Tvalue> valueExpression;
  private Channel<Tvalue> value;
  
  private AbstractTag tag
    =new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "OPTION";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      OptionState<Tvalue> state=((OptionState<Tvalue>) context.getState());

      if (state.isSelected())
      { renderAttribute(context.getWriter(),"selected",null);
      }
      if (converter!=null)
      { 
        renderAttribute
          (context.getWriter(),"value",converter.toString(state.getValue())
          );
      }
      else
      {
        if (state.getValue()!=null)
        {
          renderAttribute
            (context.getWriter()
            ,"value"
            ,state.getValue()!=null
            ?state.getValue().toString()
            :""
            );
        }
      }
      
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { Option.super.render(context);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
  };
  
  public void setValue(Expression<Tvalue> valueExpression)
  { this.valueExpression=valueExpression;
  }
  


  @Override
  public void setParent(Element parentElement)
    throws MarkupException
  { 
    super.setParent(parentElement);
    // controlGroup=parentElement.findElement(ControlGroup.class);
    // controlGroup.registerControl(getPath(),this);
  }

  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    super.bind(childUnits);
    value=getFocus().bind(valueExpression);
    if (converter==null && value!=null)
    { 
      converter=
        (StringConverter) 
        StringConverter.getInstance(value.getContentType());
    }
    if (target==null)
    { log.fine("Not bound to anything (Option)");
    }
  }
  
  public String getVariableName()
  { return null;
  }
  
  @Override
  public OptionState<Tvalue> createState()
  { return new OptionState<Tvalue>(this);
  }
  
  public void render(EventContext context)
    throws IOException
  { tag.render(context);
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {

    // Parent does all the gather work for the select list

//    
//    This is the template from TextInput.
//
//    OptionState<String> state=((OptionState<String>) context.getState());
//    
//    //System.err.println("TextInput: readPost");
//    
//    // Only update if changed
//    
//    if (state.updateValue(context.getPost().getOne(state.getVariableName())))
//    {
//      
//      if (target!=null)
//      {
//        
//        try
//        {
//          
//          if (converter!=null)
//          { target.set(converter.fromString(state.getValue()));
//          }
//          else
//          { target.set((Ttarget) state.getValue());
//          }
//        }
//        catch (AccessException x)
//        { 
//          state.setError(x.getMessage());
//          state.setException(x);
//        }
//        catch (NumberFormatException x)
//        { 
//          state.setError(x.getMessage());
//          state.setException(x);
//        }
//
//      }
//    }

  }
  
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    OptionState<Tvalue> state=((OptionState<Tvalue>) context.getState());
    if (value!=null)
    {
      try
      { 
        state.setValue(value.get());
        state.setSelected(state.getSelectState().isSelected(state.getValue()));
      }
      catch (AccessException x)
      { 
        state.setError(x.getMessage());
        state.setException(x);
      }
      catch (NumberFormatException x)
      { 
        state.setError(x.getMessage());
        state.setException(x);
      }

      
    }
    
    
  }
  
  @Override
  protected void renderError(ServiceContext context) throws IOException
  { new ErrorTag(tag).render(context);
  }
  
}

class OptionState<Tvalue>
  extends ControlState<Tvalue>
{
  private boolean selected=false;
  
  public OptionState(Option<?,?> control)
  { super(control);
  }
  
  public boolean isSelected()
  { return selected;
  }
  
  public void setSelected(boolean selected)
  { this.selected=selected;
  }
  
  public SelectState<?,Tvalue> getSelectState()
  { return (SelectState<?,Tvalue>) controlGroupState;
  }
  
  
  
}

