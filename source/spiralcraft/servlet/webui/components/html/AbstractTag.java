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

import spiralcraft.textgen.EventContext;

import java.io.IOException;
import java.io.Writer;

import spiralcraft.text.html.AttributeEncoder;

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
{
  private static final AttributeEncoder attributeEncoder
    =new AttributeEncoder();
  
  private String attributes;
  protected int contentPosition;
  
    
  protected abstract String getTagName(EventContext context);
  
  public void setAttributes(String attributes)
  { 
    this.attributes=attributes;
  }

  protected void appendAttribute(String name,String value)
  {
    if (attributes==null)
    { attributes="";
    }
    attributes=attributes+name+"=\""+value+"\" ";
    
  }

  public void setId(String val)
  { appendAttribute("id",val);
  }

  public void setClazz(String val)
  { appendAttribute("class",val);
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

  protected void renderPresentAttribute
    (Writer writer,String name,String value)
    throws IOException
  { 
    if (value!=null)
    { renderAttribute(writer,name,value);
    }
  }
  
  protected void renderAttribute(Writer writer,String name,String value)
    throws IOException
  {
    writer.write(name);
    if (value!=null)
    {
      writer.write("=\"");
      attributeEncoder.encode(value,writer);
      writer.write("\" ");
    }
    else
    { writer.write(" ");
    }
  }
  
  protected void renderAttributes(EventContext context)
    throws IOException
  { 
    if (attributes!=null)
    { context.getWriter().write(attributes+" ");
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
  protected void renderContent(EventContext context)
    throws IOException
  { 
  }
  
  /**
   * 
   * @param context
   * @throws IOException
   */
  protected void renderBefore(EventContext context)
    throws IOException
  {
  }
  
  /**
   * 
   * @param context
   * @throws IOException
   */
  protected void renderAfter(EventContext context)
    throws IOException
  {
  }

  
  /**
   * Render the Tag, and all its contents.
   * 
   * 
   * @param context
   * @throws IOException
   */
  public final void render(EventContext context)
    throws IOException
  { 
    
    boolean hasContent=hasContent();

    renderBefore(context);
    if (hasContent && contentPosition<0)
    { renderContent(context);
    }
    
    String name=getTagName(context);
    if (name!=null && name.length()>0)
    {
      Writer writer=context.getWriter();
      writer.write("<");
      writer.write(getTagName(context));
      writer.write(" ");
    
      renderAttributes(context);
    
      if (hasContent && contentPosition==0)
      { 
        writer.write(">");
   
        renderContent(context);
    
        writer.write("</");
        writer.write(getTagName(context));
        writer.write(">");
      }
      else
      { writer.write("/>");
      }

      if (hasContent && contentPosition>0)
      { renderContent(context);
      }

    }
    else
    {
      // Empty tag name suppresses tag and attributes
      if (hasContent())
      { renderContent(context);
      }
    }
    renderAfter(context);
  }
  

}
