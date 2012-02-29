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

import java.net.URI;

import javax.servlet.ServletException;

import spiralcraft.time.Clock;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;


import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;

/**
 * <p>Represents the root of a WebUI component tree
 * </p>
 * 
 * <p>The RootComponent is the Component which is addressed directly via the
 *   HTTP client and provides the UI with some control over the HTTP interaction.
 * </p>
 * 
 * 
 * 
 * @author mike
 *
 */
public class RootComponent
  extends Component
{
  private static final ClassLog log 
    = ClassLog.getInstance(RootComponent.class);
  
  public static final URI FOCUS_URI
    =URI.create("class:/spiralcraft/servlet/webui/RootComponent");

  /**
   * Find the nearest RootComponent in context
   * 
   * @param focus
   * @return
   */
  public static final Channel<RootComponent> findChannel(Focus<?> focus)
  {
    Focus<RootComponent> rcf=focus.findFocus(FOCUS_URI);
    if (rcf!=null)
    { return rcf.getSubject();
    }
    else
    { return null;
    }
  }

  private String instancePath;

  protected ThreadLocalChannel<ServiceContext> threadLocal;

  
  @Override
  @SuppressWarnings({"unchecked","rawtypes"})
  // Not using generic versions
  public final Focus<?> bind(Focus<?> focus) 
    throws ContextualException
  {
    if (debug)
    { log.fine("bind");
    }
    SimpleFocus compoundFocus
      =new SimpleFocus
        (focus
        ,new SimpleChannel(this,true)
        );
    focus=compoundFocus;
    threadLocal 
      = new ThreadLocalChannel<ServiceContext>
        (BeanReflector.<ServiceContext>getInstance(ServiceContext.class));
    compoundFocus.addFacet
      (new SimpleFocus(threadLocal));
    return super.bind(focus);
  }
  /**
   * 
   * @param contextRelativePath The path of this UIComponent relative
   *   to the containing ServletContext, used to differentiate between
   *   multiple components read from the same WebUI file.
   */
  void setInstancePath(String contextRelativePath)
  { this.instancePath=contextRelativePath;
  }
  
  /**
   * 
   * @return The path of this UIComponent relative
   *   to the containing ServletContext
   */
  public String getInstancePath()
  { return instancePath;
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

  @Override
  public void message
    (Dispatcher context,Message message)
  {
    if (threadLocal==null)
    { throw new RuntimeException("UIComponent "+this+" never bound");
    }
    
    threadLocal.push((ServiceContext) context);
    try
    { super.message(context,message);
    }
    finally
    { threadLocal.pop();
    }
    
  }
  
  
  public Command<?,?,?> actionCommand(final String actionName)
  {
    Command<ServiceContext,Void,Void> ret
      =new CommandAdapter<ServiceContext,Void,Void>()
      { 
        @Override
        public void run()
        { getTarget().queueAction(actionName);
        }
      };
    ret.setTarget(threadLocal.get());
    return ret;
  }
  
  public Command<Void,Void,Void> redirectCommand(final String redirectURI)
  {
    return new CommandAdapter<Void,Void,Void>()
    {
      { name="redirect";
      }
          
      @Override
      public void run()
      { 
        try
        { threadLocal.get().redirect(URI.create(redirectURI));
        }
        catch (ServletException x)
        { log.log(Level.WARNING,"Threw exception on redirect",x);
        }
      }
  
    };
  } 

}

