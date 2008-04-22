package spiralcraft.servlet.webui.components.html;

import java.io.IOException;
import java.util.List;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.util.ArrayToString;
import spiralcraft.util.StringArrayToString;
import spiralcraft.util.StringConverter;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Element;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

public class TextInput<Ttarget>
  extends Control<Ttarget>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(TextInput.class);
  
  private String name;
  private StringConverter<Ttarget> converter;
  private boolean password;
  
  /**
   * Whether the control is in password mode
   * 
   * @param password
   */
  public void setPassword(boolean password)
  { this.password=password;
  }
  
  private AbstractTag tag
    =new AbstractTag()
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
      ControlState<String> state=((ControlState<String>) context.getState());
      if (password)
      { renderAttribute(context.getWriter(),"type","password");
      }
      else
      { renderAttribute(context.getWriter(),"type","text");
      }
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      renderAttribute(context.getWriter(),"value",state.getValue());
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return false;
    }
    
  };
  
  private ErrorTag errorTag=new ErrorTag(tag);
  
  public void setName(String name)
  { this.name=name;
  }
  
  public AbstractTag getTag()
  { return tag;
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
    if (converter==null && target!=null)
    { 
      Class targetClass=target.getContentType();
      if (targetClass.isArray())
      { 
        if (targetClass.getComponentType().equals(String.class))
        { 
          converter=(StringConverter<Ttarget>) new StringArrayToString();
          ((StringArrayToString) converter).setTrim(true);
        }
        else
        { 
          converter=new ArrayToString(targetClass.getComponentType());
        }
      }
      else
      {
        converter=
          (StringConverter<Ttarget>) 
          StringConverter.getInstance(target.getContentType());
      }
    }
    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
  }
  
  public String getVariableName()
  { return name;
  }
  
  @Override
  public ControlState<String> createState()
  { return new ControlState<String>(this);
  }
  
  public void render(EventContext context)
    throws IOException
  { 
    if ( ((ControlState<?>) context.getState()).isErrorState())
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
    ControlState<String> state=((ControlState<String>) context.getState());
    //System.err.println("TextInput: readPost");
    
    // Only update if changed
    if (context.getPost()!=null
        && state.updateValue(context.getPost().getOne(state.getVariableName()))
       )
    {
    
      if (target!=null)
      {
        
        try
        {
          
          String val=state.getValue();
          if (converter!=null && val!=null)
          { target.set(converter.fromString(state.getValue()));
          }
          else
          { target.set((Ttarget) val);
          }
          
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
        catch (IllegalArgumentException x)
        { 
          state.setError(x.getMessage());
          state.setException(x);
        }

      }
    }

  }
  
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    ControlState<String> state=((ControlState<String>) context.getState());
    if (target!=null)
    {
      try
      {
        Ttarget val=target.get();
        if (debug)
        { log.fine(toString()+" scattering "+val);
        }
        if (val!=null)
        {
          
          if (converter!=null)
          { state.setValue(converter.toString(val));
          }
          else
          { state.setValue(val.toString());
          }
        }
        else
        { state.setValue(null);
        }
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

}

