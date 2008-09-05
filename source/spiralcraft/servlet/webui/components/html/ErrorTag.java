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
import java.io.Writer;

import spiralcraft.servlet.webui.ControlState;
import spiralcraft.textgen.EventContext;

/**
 * <p>A Tag that formats an error message that is associated with another
 *   html based component. By default, this tag will render before
 *   the specified component.
 * </p>
 * 
 * @author mike
 *
 */
public class ErrorTag
    extends AbstractTag
{
  private AbstractTag controlTag;
  private String tagName=null;
  
  public ErrorTag(AbstractTag controlTag)
  { this.controlTag=controlTag;
  }

  @Override
  protected String getTagName(EventContext context)
  { 
    if (((ControlState<?>) context.getState()).getErrors()!=null)
    { return tagName;
    }
    else
    { return null;
    }
  }

  /**
   * <p>Specify the tag name that will be output when there is an error.
   * </p>
   * 
   * <p>If left unset (default), no tag will be rendered, but any 
   *   content will still be rendered.
   * </p>
   * 
   * @param tagName
   */
  public void setTagName(String tagName)
  { this.tagName=tagName;
  }
  
    
  @Override
  protected boolean hasContent()
  { return true;
  }
    
  @Override
  protected void renderAfter(EventContext context)
    throws IOException
  {
    if (controlTag!=null)
    { controlTag.render(context);
    }
  }
  
  @Override
  protected void renderContent(EventContext context)
    throws IOException
  { 
    ControlState<?> state=(ControlState<?>) context.getState();
    Writer out=context.getWriter();
    
    String[] errors=state.getErrors();
    if (errors!=null)
    { 
      for (String string: errors)
      { 
        out.write(string);
        out.write("<br/>");
      }
    }
    if (state.getException()!=null)
    { 
      out.write("<!--\r\n");

      Throwable exception=state.getException();
      while (exception!=null)
      {
        out.write(exception.toString());

        for (StackTraceElement element : exception.getStackTrace())
        {
          out.write(element.toString());
          out.write("\r\n");
        }
        exception=exception.getCause();
      }
      out.write("-->\r\n");
      
    }
    
  }


  @Override
  protected void renderAttributes(EventContext context)
    throws IOException
  { 
    super.renderAttributes(context);
  }
  

}
