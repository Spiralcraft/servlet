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
package spiralcraft.servlet.webui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;
import spiralcraft.textgen.compiler.TglUnit;
import spiralcraft.time.Clock;

import spiralcraft.lang.BindException;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLogger;


/**
 * <p>Represents the root of a WebUI component tree
 * </p>
 * 
 * <p>The UiComponent is the Component which is addressed directly via the HTTP
 *   client and provides the UI with some control over the HTTP interaction.
 * </p>
 * 
 * 
 * 
 * @author mike
 *
 */
public class UIComponent
  extends Component
{
  private static final ClassLogger log 
    = ClassLogger.getInstance(UIComponent.class);
  
  private Focus<?> focus;
  private String contextRelativePath;
  protected ThreadLocalChannel<ServiceContext> threadLocal;
  
  public UIComponent(Focus<?> focus)
  { 
    this.focus=focus;
    
    
    

  }
  
  @SuppressWarnings("unchecked")
  // Not using generic versions
  public final void bind(List<TglUnit> childUnits) 
    throws BindException,MarkupException
  {
    log.fine("bind");
    threadLocal 
      = new ThreadLocalChannel<ServiceContext>
        (BeanReflector.<ServiceContext>getInstance(ServiceContext.class));
    focus=new CompoundFocus(focus,threadLocal);
    super.bind(childUnits);
  }
  /**
   * 
   * @param contextRelativePath The path of this UIComponent relative
   *   to the containing ServletContext
   */
  void setContextRelativePath(String contextRelativePath)
  { this.contextRelativePath=contextRelativePath;
  }
  
  /**
   * 
   * @return The path of this UIComponent relative
   *   to the containing ServletContext
   */
  public String getContextRelativePath()
  { return contextRelativePath;
  }
  
  @Override
  public Focus<?> getFocus()
  { return focus;
  }
  
  /**
   * @return The mime type of the content returned by this
   *   Component
   */
  public String getContentType()
  { return "text/html";
  }
  
  /**
   * Returns the time this UI was last modified. By default,
   *   returns the current time. Override to return a specific
   *   time for resources that are not regenerated for each
   *   request.
   * 
   * @return The last modified time in milliseconds since 1970.
   */
  public long getLastModified()
  { return Clock.instance().approxTimeMillis();
  }

  public void message
    (EventContext context,Message message,LinkedList<Integer> path)
  {
    if (threadLocal==null)
    { throw new RuntimeException("UIComponent "+this+" never bound");
    }
    
    threadLocal.push((ServiceContext) context);
    try
    { super.message(context,message,path);
    }
    finally
    { threadLocal.pop();
    }
    
  }
  
  public void render(EventContext context)
    throws IOException
  { 
    threadLocal.push((ServiceContext) context);
    try
    { super.render(context);
    }
    finally
    { threadLocal.pop();
    }
  }

}

