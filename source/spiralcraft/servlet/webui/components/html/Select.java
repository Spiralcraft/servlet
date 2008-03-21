package spiralcraft.servlet.webui.components.html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.util.ArrayUtil;
import spiralcraft.util.StringConverter;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Element;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

public class Select<Ttarget,Tvalue>
  extends ControlGroup<Ttarget>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(Select.class);
  
  private String name;
  private StringConverter<Ttarget> converter;
//  private StringConverter<Tvalue> valueConverter;
  private Channel<?> source;
  private Expression<?> sourceExpression;
  private boolean multi=false;
  
  
  private AbstractTag tag
    =new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "SELECT";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<String> state=((ControlState<String>) context.getState());
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      if (multi)
      { renderAttribute(context.getWriter(),"multiple",null);
      }
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    protected void renderContent(EventContext context)
      throws IOException
    { Select.super.render(context);
    }    
    
  };

  private ErrorTag errorTag
    =new ErrorTag(tag);
  
  public AbstractTag getTag()
  { return tag;
  }
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setSource(Expression<?> sourceExpression)
  { this.sourceExpression=sourceExpression;
  }


  @Override
  public void setParent(Element parentElement)
    throws MarkupException
  { 
    super.setParent(parentElement);
  }

  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public Channel<Ttarget> extend(Focus<?> parentFocus)
    throws BindException
  { 
    Channel<Ttarget> target=(Channel<Ttarget>) super.extend(parentFocus);
    if (converter==null && target!=null)
    { 
      converter=
        (StringConverter<Ttarget>) 
        StringConverter.getInstance(target.getContentType());
    }
    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
    source=parentFocus.bind(sourceExpression);
    return target;
  }
  
  @SuppressWarnings("unchecked")
  public Focus<?> bindSelf()
  { 
    Focus<?> focus=new CompoundFocus(getFocus(),source);
    return focus; 
  }
  
  
  public String getVariableName()
  { return name;
  }
  
  @SuppressWarnings("unchecked")
  public void render(EventContext context)
    throws IOException
  {
    if (((ControlState<Ttarget>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }
  
//  void setValueConverter(StringConverter<Tvalue> converter)
//  { this.valueConverter=converter;
//  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    SelectState<Ttarget,Tvalue> state
      =((SelectState<Ttarget,Tvalue>) context.getState());
    
    if (multi)
    {
      log.fine("Multiselect not implemented");
    }
    else
    {
      Ttarget val;
      try
      {
        List<String> strings=context.getPost().get(state.getVariableName());
        if (debug)
        { log.fine("Read ["+strings+"] from posted formvar "+state.getVariableName());
        }
        if (strings==null || strings.size()==0)
        { val=null;
        }
        else if (strings.get(0)!=null && !(strings.get(0).length()==0))
        { val=converter.fromString(strings.get(0));
        }
        else
        { val=null;
        }
        if (debug)
        { log.fine("Got selection ["+val+"] for "+getVariableName());
        }

        state.setValue(val);
        if (target!=null)
        { target.set(val);
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
  
  public String toString()
  { return super.toString()+": name="+name;
  }
  
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void scatter(ServiceContext context)
  {
    SelectState<Ttarget,Tvalue> state=
      ((SelectState<Ttarget,Tvalue>) context.getState());
    if (target!=null)
    {
      try
      {
        state.setValue(target.get());
        
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
  public SelectState<Ttarget,Tvalue> createState()
  { return new SelectState<Ttarget,Tvalue>(this);
  }  
}

class SelectState<Ttarget,Tvalue>
  extends ControlGroupState<Ttarget>
{
  
  public List<Tvalue> selected;
  
  public SelectState(Select<Ttarget,Tvalue> control)
  { super(control);
  }
  
  @SuppressWarnings("unchecked")
  public void setValue(Ttarget targetVal)
  {
    super.setValue(targetVal);
    if (targetVal==null)
    { selected=null;
    }
    else
    {
      selected=new ArrayList<Tvalue>();
      // XXX Figure out cardinality beforehand
      if (targetVal instanceof Iterable)
      {
        for (Tvalue val : (Iterable<Tvalue>) targetVal)
        { selected.add(val);
        }
      }
      else if (targetVal.getClass().isArray())
      { 
        for (Tvalue val : ArrayUtil.<Tvalue>iterable((Tvalue[]) targetVal))
        { selected.add(val);
        }
      }
      else
      { selected.add((Tvalue) targetVal);
      }
    }
  }
  
  public boolean isSelected(Tvalue value)
  { 
    boolean ret;
    if (selected==null)
    { ret=false;
    }
    else
    { ret=selected.contains(value);
    }
    
    if (control.isDebug())
    { log.fine(control.toString()+": "+(ret?"SELECTED":"not selected")+" value="+value+" selected="+selected);
    }
    return ret;
  }
  
  
}
