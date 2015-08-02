//
//Copyright (c) 2011 Michael Toth
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

import java.util.LinkedList;

import spiralcraft.app.Component;
import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.kit.AbstractMessageHandler;
import spiralcraft.common.ContextualException;
//import spiralcraft.data.Tuple;
//import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.textgen.PrepareMessage;
//import spiralcraft.ui.MetadataType;

/**
 * <p>An area for editing a particular data element, which includes a set
 *   of input controls, a label, and message areas. 
 * </p>
 * 
 * @author mike
 *
 * @param <T>
 */
public class FormField<T>
  extends ControlGroup<T>
{
  
  static class State<T>
    extends ControlGroupState<T>
  {

    String inputId;
    
    public State(ControlGroup<T> controlGroup)
    { super(controlGroup);
    }
    
    
  }
  
  public class Tag 
    extends AbstractTag
  {
    { addStandardClass("sc-webui-form-field");
    }
    
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "div";
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
  }
  
  class InputHandler
    extends AbstractMessageHandler
  {
    { type=PrepareMessage.TYPE;
    }

    @Override
    protected void doHandler(
      Dispatcher dispatcher,
      Message message,
      MessageHandlerChain next)
    { 
      getState().inputId=((ComponentState) dispatcher.getState()).getId();
      next.handleMessage(dispatcher,message);
    }
  }
  
  private String label;
  private Tag tag=new Tag();
  private ErrorTag errorTag=new ErrorTag();
  private boolean renderTag=true;

  public InputHandler newInputHandler()
  { return new InputHandler();
  }
  
  /**
   * Render an html tag (a "div" by default) for this component. Defaults
   *   to true.
   * 
   * @param renderTag
   */
  public void setRenderTag(boolean renderTag)
  { this.renderTag=renderTag;
  }
    
  /**
   * Adds a Label element as a child that contains the specified text.
   * @param label
   */
  public void setLabel(String label)
  { this.label=label;
  }
  
  @Override
  protected void addHandlers()
    throws ContextualException
  { 
    if (this.renderTag)
    {
      addHandler(errorTag);
      addHandler(tag);
    }
    super.addHandlers();
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  public String getInputId()
  { return getState().inputId;
  }
  
  @Override
  public State<T> createState()
  { return new State<T>(this);
  }
  
  /**
   * The id of the input control contained in this FormField
   * 
   * @param inputId
   */
  public void setInputId(String inputId)
  { getState().inputId=inputId;
  }
  
  @Override
  public State<T> getState()
  { return (State<T>) super.getState();
  }
  
  @Override
  protected LinkedList<Component> addFirstBoundChildren
    (Focus<?> focus,LinkedList<Component> children)
    throws ContextualException
  {
    if (label!=null)
    {
      if (children==null)
      { children=new LinkedList<Component>();
      }
      Label labelElement=new Label();
      labelElement.setText(label);
      labelElement.bind(focus);
      children.add(labelElement);
    }
    return children;
  }
  
}
