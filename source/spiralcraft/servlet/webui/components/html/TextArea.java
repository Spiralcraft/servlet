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
import java.util.List;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.util.StringConverter;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

public class TextArea<Ttarget>
  extends Control<Ttarget>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(TextInput.class);
  
  private String name;
  private StringConverter<Ttarget> converter;
  private boolean required;
  
  private Tag tag=new Tag();
  
  private ErrorTag errorTag=new ErrorTag(tag);
  
  public void setName(String name)
  { this.name=name;
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }

  public void setRequired(boolean required)
  { this.required=required;
  }
 

  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    super.bind(childUnits);
    if (converter==null && target!=null)
    { 
      converter=
        (StringConverter<Ttarget>) 
        StringConverter.getInstance(target.getContentType());
    }
    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
  }
  
  @Override
  public String getVariableName()
  { return name;
  }
  
  @Override
  public ControlState<String> createState()
  { return new ControlState<String>(this);
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    if ( ((ControlState<?>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }
  
  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public void gather(ServiceContext context)
  {
    ControlState<String> state=((ControlState<String>) context.getState());
    //System.err.println("TextInput: readPost");
    
    // Only update if changed
    if (context.getPost()!=null)
    {
    
      String postVal=context.getPost().getOne(state.getVariableName());
      if (debug)
      { log.fine("Got posted value "+postVal);
      }
      // Empty strings should be null.
      if (postVal!=null && postVal.length()==0)
      { postVal=null;
      }

      if (required && postVal==null)
      { 
        
        if (debug)
        { log.fine("Failed required test");
        }
        state.addError("Input required");
      }
      else if (state.updateValue(postVal))
      {
    
        if (target!=null)
        {
          
          try
          {
          
            String val=state.getValue();
          
            Ttarget tval=null;
            if (converter!=null && val!=null)
            { tval=converter.fromString(val);
            }
            else
            { tval=(Ttarget) val;
            }
            if (inspect(tval,state))
            { target.set(tval);
            }
          
          }
          catch (AccessException x)
          { state.setException(x);
          }
          catch (NumberFormatException x)
          { state.setException(x);
          }
          catch (IllegalArgumentException x)
          { state.setException(x);
          }

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
      { state.setException(x);
      }
      catch (NumberFormatException x)
      { state.setException(x);
      }

      
    }
  }

  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return "textarea";
    }

    @SuppressWarnings("unchecked") // Generic cast
    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<String> state=((ControlState<String>) context.getState());
      renderAttribute(context.getWriter(),"type","text");
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      renderAttribute(context.getWriter(),"value",state.getValue());
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { 
      Ttarget value=((ControlState<Ttarget>) context.getState()).getValue();
      if (value!=null)
      { 
        if (converter!=null)
        { context.getWriter().write(converter.toString(value));
        }
        else
        { context.getWriter().write(value.toString());
        }
      }
    }

    public void setRows(String val)
    { appendAttribute("rows",val);
    }
  
    public void setCols(String val)
    { appendAttribute("cols",val);
    }
  
    public void setDisabled(String val)
    { appendAttribute("disabled",val);
    }
  
    public void setName(String val)
    { appendAttribute("name",val);
    }
  
    public void setReadOnly(String val)
    { appendAttribute("readonly",val);
    }
    
    
  }
  
}

