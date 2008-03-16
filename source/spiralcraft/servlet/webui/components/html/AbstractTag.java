//
//Copyright (c) 1998,2007 Michael Toth
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

public abstract class AbstractTag
{
  private static final AttributeEncoder attributeEncoder
    =new AttributeEncoder();
  
  private String attributes;
    
  protected abstract String getTagName(EventContext context);
  
  public void setAttributes(String attributes)
  { 
    this.attributes=attributes;
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
   * Override to indicate whether this tag should render itself as an open
   *   tag and call renderContent(), or whether this tag should close itself
   *   
   * @return
   */
  protected abstract boolean hasContent();
  
  protected void renderContent(EventContext context)
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
    Writer writer=context.getWriter();
    writer.write("<");
    writer.write(getTagName(context));
    writer.write(" ");
    
    renderAttributes(context);
    
    if (hasContent())
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
  }
}
