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
package spiralcraft.servlet.webui.components;



import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.servlet.ServletException;


import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLogger;

import spiralcraft.security.auth.AuthSession;

import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.text.html.URLDataEncoder;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;


/**
 * <P>Secures a block of content by redirecting to another page if the user
 *   is not authenticated.
 * </P>
 * 
 * <P>Requires that a spiralcraft.security.auth.AuthSession be published
 *   in the Focus chain.
 * </P>
 *
 * <P>Sets the focus to the AuthSession, and publishes a self reference
 *   to provide access to the logoutCommand().
 * </P>
 * 
 * @author mike
 *
 */
public class Guard
  extends Component
{
  private static final ClassLogger log
    =ClassLogger.getInstance(Guard.class);

  private Channel<AuthSession> sessionChannel;
  private URI loginURI;
  private CompoundFocus<?> focus;

  public Focus<?> getFocus()
  { return focus;
  }

  public void setLoginURI(URI uri)
  { loginURI=uri;
  }
  
  @SuppressWarnings("unchecked")
  protected void setupSession(Focus<?> parentFocus)
  {
    Focus<AuthSession> sessionFocus
      =(Focus<AuthSession>) parentFocus.findFocus(AuthSession.FOCUS_URI);
    if (sessionFocus!=null)
    { sessionChannel=sessionFocus.getSubject();
    }
  }

  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    Focus<?> parentFocus=getParent().getFocus();
    setupSession(parentFocus);
    focus=new CompoundFocus(parentFocus,sessionChannel);
    focus.bindFocus("spiralcraft.builder", getAssembly().getFocus());
    super.bind(childUnits);
  }  
  

  private void setupRedirect(ServiceContext context)
    throws ServletException
  {
    String referer=context.getRequest().getRequestURL().toString();
    URI redirect
      =URI.create
        (loginURI.getPath()+"?referer="+URLDataEncoder.encode(referer));
    if (debug)
    { log.fine("Setting up redirect to "+redirect);
    }
    context.redirect(redirect);
  }
  
  public Command<Void,Void> logoutCommand()
  {     
    return new CommandAdapter<Void,Void>()
      { 
        public void run()
        { logout();
        }
      };
  }
    
  private void logout()
  {
    sessionChannel.get().logout();
  }
  
  protected void handlePrepare(ServiceContext context)
  { 
    super.handlePrepare(context);
    if (!sessionChannel.get().isAuthenticated())
    { 
      try
      { setupRedirect(context);
      }
      catch (ServletException x)
      { x.printStackTrace();
      }
    }
    
  }
  
  public void render(EventContext context)
    throws IOException
  {
    if (!sessionChannel.get().isAuthenticated())
    { 
      try
      { setupRedirect((ServiceContext) context);
      }
      catch (ServletException x)
      { 
        x.printStackTrace();
        context.getWriter().write(x.toString());
      }
    }
    else
    { super.render(context);
    }
  }

}



