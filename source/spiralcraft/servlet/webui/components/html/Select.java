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
import java.util.ArrayList;
import java.util.List;


import spiralcraft.util.ArrayUtil;
import spiralcraft.util.StringConverter;

import spiralcraft.textgen.EventContext;

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

/**
 * <P>A standard HTML SELECT list, bound to a target and a source.
 * </P>
 * 
 * <P>The "x" (binding target) property contains an expression that references
 *   the currently selected item(s). The optional "source" property contains an 
 *   expression that provides a list of candidate values, used for generating 
 *   the set of options (see the Option class).
 * </P>
 *  
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
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
    { return "select";
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
    
    @Override
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
  @SuppressWarnings("unchecked") // Not using generic versions
  public Channel<Ttarget> bindTarget(Focus<?> parentFocus)
    throws BindException
  { 
    Channel<Ttarget> target=(Channel<Ttarget>) super.bindTarget(parentFocus);
    if (converter==null && target!=null)
    { 
      converter=
        (StringConverter<Ttarget>) 
        StringConverter.getInstance(target.getContentType());
    }
    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
    
    if (sourceExpression!=null)
    { source=parentFocus.bind(sourceExpression);
    }
    return target;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public Focus<?> bindExports()
  { 
    Focus<?> focus=new CompoundFocus(getFocus(),source);
    return focus; 
  }
  
  
  @Override
  public String getVariableName()
  { return name;
  }
  
  @Override
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
        if (context.getPost()==null)
        { return;
        }
        
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
      { state.setException(x);
      }
      catch (NumberFormatException x)
      { state.setException(x);
      }
    }


  }
  
  @Override
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
      { state.setException(x);
      }
      catch (NumberFormatException x)
      { state.setException(x);
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
  
  @Override
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

