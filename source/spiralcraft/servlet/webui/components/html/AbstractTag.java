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

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.kit.AbstractMessageHandler;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.DictionaryBinding;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.textgen.OutputContext;
import spiralcraft.textgen.RenderMessage;

import java.io.IOException;

import spiralcraft.text.xml.AttributeEncoder;
import spiralcraft.util.ArrayUtil;

/**
 * <p>A representation of an HTML 4.0 tag for use by components that render
 *   HTML tags.
 * </p>
 * 
 * <p>Renders a set of HTML 4.0 standard attributes and script hook attributes
 *   (intrinsic events) that can be supplied as bean properties in
 *   configuration/UI code and passed through.
 * </p>
 * 
 * <p>Typically incorporated as an inner subclass into components and 
 *   delegated to for tag rendering, where the methods renderAttributes(), 
 *   hasContent() and renderContent() are overridden to complete the use case.
 * </p> 
 * 
 * @author mike
 *
 */
public abstract class AbstractTag
  extends AbstractMessageHandler
{
  
  private static final AttributeEncoder attributeEncoder
    =new AttributeEncoder();
  
  private String attributes;
  protected boolean generateId;
  protected int contentPosition;
  protected int tagPosition;
  protected boolean shouldRender=true;
  protected boolean addNewLine;
    
  protected abstract String getTagName(Dispatcher context);
  
  private DictionaryBinding<?>[] attributeBindings;
  private DictionaryBinding<?>[] standardAttributeBindings;
  
  { type=RenderMessage.TYPE;
  }
  
  
  /**
   * <p>Set to false if the tag should not render. The associated
   *   control will still process input, however.
   * </p>
   * 
   * <p>Useful for cases where the actual output tag is implemented
   *   using custom markup but the control should still be enabled.
   * </p>
   * @param shouldRender
   */
  public void setShouldRender(boolean shouldRender)
  { this.shouldRender=shouldRender;
  }
  
  public void setAttributes(String attributes)
  { 
    this.attributes=attributes;
  }

  public void setTagPosition(int position)
  { this.tagPosition=position;
  }
  
  public void setAttributeBindings(DictionaryBinding<?>[] attributeBindings)
  { this.attributeBindings=attributeBindings;
  }
  
  
  @Override
  protected void doHandler
    (Dispatcher context,Message message,MessageHandlerChain next)
  { 
    try
    { render(context,message,next);
    }
    catch (IOException x)
    { throw new RuntimeException(x);
    }
  }
  
  
  /**
   * Render the Tag, and all its contents.
   * 
   * 
   * @param context
   * @throws IOException
   */
  private final void render
    (Dispatcher context,Message message,MessageHandlerChain next)
    throws IOException
  { 
    
    boolean hasContent=hasContent();

    renderBefore(context,message,next);
    if (hasContent && contentPosition<0)
    { renderContent(context,message,next);
    }
    
    String name=getTagName(context);
    if (shouldRender && name!=null && name.length()>0)
    {
      Appendable out=OutputContext.get();
      if (addNewLine)
      { out.append("\r\n");
      }
      out.append("<");
      out.append(getTagName(context));
    
      renderAttributes(context,out);
    
      if (hasContent && contentPosition==0)
      { 
        out.append(">");
        if (addNewLine)
        { out.append("\r\n");
        }
   
        renderContent(context,message,next);
    
        if (addNewLine)
        { out.append("\r\n");
        }
        out.append("</");
        out.append(getTagName(context));
        out.append(">");
      }
      else
      { out.append(" />");
      }
      if (addNewLine)
      { out.append("\r\n");
      }

      if (hasContent && contentPosition>0)
      { renderContent(context,message,next);
      }

    }
    else
    {
      // Rendering disabled
      if (hasContent())
      { renderContent(context,message,next);
      }
    }
    renderAfter(context,message,next);
  }  
  
  /**
   * 
   * @param context
   * @throws IOException
   */
  protected void renderContent(Dispatcher context,Message message,MessageHandlerChain next)
    throws IOException
  { 
    if (tagPosition==0)
    { next.handleMessage(context,message);
    }
  }
  
  protected void appendAttribute(String name,String value)
  {
    if (attributes==null)
    { attributes="";
    }
    attributes=attributes+name+"=\""+value+"\" ";
    
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void addStandardBinding(String name,Expression expr)
  { 
    DictionaryBinding<?> binding=new DictionaryBinding();
    binding.setName(name);
    binding.setTarget(expr);
    
    if (standardAttributeBindings!=null)
    {
      standardAttributeBindings
        =ArrayUtil.append
          (standardAttributeBindings, binding);
    }
    else
    { standardAttributeBindings=new DictionaryBinding[] {binding};
    }
  }

  public void setId(String val)
  { appendAttribute("id",val);
  }
  
  public void setIdX(Expression<?> expr)
  { addStandardBinding("id",expr);
  }

  public void setClazz(String val)
  { appendAttribute("class",val);
  }
  
  public void setClassX(Expression<?>[] exprA)
  { 
    if (exprA!=null)
    {
      for (Expression<?> expr:exprA)
      { addStandardBinding("class",expr);
      }
    }
  }

  public void setStyle(String val)
  { appendAttribute("style",val);
  }
  
  public void setTitle(String val)
  { appendAttribute("title",val);
  }
  
  public void setDir(String val)
  { appendAttribute("dir",val);
  }
  
  public void setLang(String val)
  { appendAttribute("lang",val);
  }
  
  public void setTabindex(String val)
  { appendAttribute("tabindex",val);
  }
  
  public void setAccesskey(String val)
  { appendAttribute("accesskey",val);
  }

  public void setOnload(String val)
  { appendAttribute("onload",val);
  }
  
  public void setOnunload(String val)
  { appendAttribute("onunload",val);
  }

  public void setOnclick(String val)
  { appendAttribute("onclick",val);
  }
  
  public void setOnclickX(Expression<?> expr)
  { addStandardBinding("onclick",expr);
  }

  public void setOndblclick(String val)
  { appendAttribute("ondblclick",val);
  }

  public void setOnmousedown(String val)
  { appendAttribute("onmousedown",val);
  }

  public void setOnmouseup(String val)
  { appendAttribute("onmouseup",val);
  }

  public void setOnmouseover(String val)
  { appendAttribute("onmouseover",val);
  }
  
  public void setOnmousemove(String val)
  { appendAttribute("onmousemove",val);
  }

  public void setOnmouseout(String val)
  { appendAttribute("onmouseout",val);
  }

  public void setOnfocus(String val)
  { appendAttribute("onfocus",val);
  }
  
  public void setOnblur(String val)
  { appendAttribute("onblur",val);
  }
  
  public void setOnkeypress(String val)
  { appendAttribute("onkeypress",val);
  }

  public void setOnkeydown(String val)
  { appendAttribute("onkeydown",val);
  }

  public void setOnkeyup(String val)
  { appendAttribute("onkeyup",val);
  }

  public void setOnsubmit(String val)
  { appendAttribute("onsubmit",val);
  }
  
  public void setOnreset(String val)
  { appendAttribute("onreset",val);
  }
  
  public void setOnselect(String val)
  { appendAttribute("onselect",val);
  }

  public void setOnchange(String val)
  { appendAttribute("onchange",val);
  }
  
  public void setAutocomplete(String val)
  { appendAttribute("autocomplete",val);
  }
  
  public void setContentPosition(int contentPosition)
  { this.contentPosition=contentPosition;
  }

  @Override
  public Focus<?> bind(Focus<?> focus)
    throws ContextualException
  { 
    if (attributeBindings!=null)
    {
      for (DictionaryBinding<?> binding:attributeBindings)
      { binding.bind(focus);
      }
    }
    if (standardAttributeBindings!=null)
    {
      for (DictionaryBinding<?> binding:standardAttributeBindings)
      { binding.bind(focus);
      }
    }
    return focus;
  }
  
  protected void renderPresentAttribute
    (Appendable out,String name,String value)
    throws IOException
  { 
    if (value!=null)
    { renderAttribute(out,name,value);
    }
  }
  
  /**
   * <p>Encode and render the specified attribute/value pair
   * </p>
   * 
   * @param writer
   * @param name
   * @param value
   * @throws IOException
   */
  protected void renderAttribute(Appendable out,String name,String value)
    throws IOException
  {
    out.append(" ");
    out.append(name);
    if (value!=null)
    {
      out.append("=\"");
      attributeEncoder.encode(value,out);
      out.append("\"");
    }
  }
  
  protected void renderAttributes(Dispatcher context,Appendable out)
    throws IOException
  { 
    if (generateId)
    { renderId(out,context);
    }
    if (attributes!=null)
    { out.append(" "+attributes);
    }
    if (standardAttributeBindings!=null)
    { renderBoundAttributes(out,standardAttributeBindings);
    }
    if (attributeBindings!=null)
    { renderBoundAttributes(out,attributeBindings);
    }
  }

  protected void renderId(Appendable out,Dispatcher context)
    throws IOException
  {
    ComponentState state=(ComponentState) context.getState();
    if (state!=null)
    { renderAttribute(out,"id",state.getId());
    }
  }
    
  protected void renderBoundAttributes
    (Appendable out,DictionaryBinding<?>[] bindings)
    throws IOException
  {
    for (DictionaryBinding<?> binding : bindings)
    { 
      String val=binding.get();
      if (val!=null)
      { renderAttribute(out,binding.getName(),val);
      }
    }
  }
  
  /**
   * <p> Override to indicate whether this tag should render itself as an open
   *   tag and call renderContent(), or whether this tag should close itself
   * </p>
   * 
   * @return Whether the tag has content.
   */
  protected abstract boolean hasContent();
  
  /**
   * 
   * @param context
   * @throws IOException
   */
  protected void renderBefore
    (Dispatcher context
    ,Message message
    ,MessageHandlerChain next
    )
    throws IOException
  { 
    if (tagPosition>0)
    { next.handleMessage(context,message);
    }    
  }
  
  /**
   * 
   * @param context
   * @throws IOException
   */
  protected void renderAfter
    (Dispatcher context
    ,Message message
    ,MessageHandlerChain next
    )
    throws IOException
  {
    if (tagPosition<0)
    { next.handleMessage(context,message);
    }
  }


  

}
