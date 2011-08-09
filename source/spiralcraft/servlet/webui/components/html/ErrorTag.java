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

import spiralcraft.app.Message;
import spiralcraft.app.State;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.textgen.OutputContext;
import spiralcraft.app.Dispatcher;
import spiralcraft.app.MessageHandlerChain;

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
  { tagPosition=-1;
  }
  
  private String tagName=null;
  

  @Override
  protected String getTagName(Dispatcher context)
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
  protected void renderContent
    (Dispatcher context
    ,Message message
    ,MessageHandlerChain next
    )
    throws IOException
  { 
    ControlState<?> state=(ControlState<?>) context.getState();
    Appendable out=OutputContext.get();
    
    String[] errors=state.getErrors();
    if (errors!=null)
    { 
      for (String string: errors)
      { 
        out.append(string);
        out.append("<br/>");
      }
    }
    if (state.getException()!=null)
    { 
      out.append("<!--\r\n");

      Throwable exception=state.getException();
      while (exception!=null)
      {
        out.append(exception.toString());

        for (StackTraceElement element : exception.getStackTrace())
        {
          out.append(element.toString());
          out.append("\r\n");
        }
        exception=exception.getCause();
      }
      out.append("-->\r\n");
      
    }
    super.renderContent(context,message,next);
    
  }
  
  @Override
  protected boolean shouldHandleMessage(Dispatcher context,Message message)
  { 
    if (super.shouldHandleMessage(context,message))
    {
      State st=context.getState();
      if (st!=null && st instanceof ControlState)
      { return ((ControlState<?>) st).isErrorState();
      }
    }
    return false;
  }
}
