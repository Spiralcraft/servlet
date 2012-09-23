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

import java.net.URI;

import javax.servlet.ServletException;


import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;

import spiralcraft.security.auth.AuthSession;

import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.text.html.URLDataEncoder;

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.InitializeMessage;

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

  private Channel<AuthSession> sessionChannel;
  private URI loginURI;



  public void setLoginURI(URI uri)
  { loginURI=uri;
  }
  

  protected void setupSession(Focus<?> parentFocus)
  {
    Focus<AuthSession> sessionFocus
      =parentFocus.<AuthSession>findFocus(AuthSession.FOCUS_URI);
    if (sessionFocus!=null)
    { sessionChannel=sessionFocus.getSubject();
    }
  }

  @Override
  protected Focus<?> bindStandard(Focus<?> focus)
    throws ContextualException
  { 
    setupSession(focus);
    focus=focus.chain(sessionChannel);
    focus.addFacet(getAssembly().getFocus());
    return super.bindStandard(focus);
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
  
  public Command<Void,Void,Void> logoutCommand()
  {     
    return new CommandAdapter<Void,Void,Void>()
      { 
        @Override
        public void run()
        { logout();
        }
      };
  }
    
  private void logout()
  {
    sessionChannel.get().logout();
  }
  
  @Override
  protected void handlePrepare(ServiceContext context)
  { 
    super.handlePrepare(context);
    if (!permitted())
    { 
      if (debug)
      { 
        
        log.fine
          (getLogPrefix(context)+"Not permitted "+sessionChannel.get());
      }
      try
      { setupRedirect(context);
      }
      catch (ServletException x)
      { x.printStackTrace();
      }
    }
    
  }

  @Override
  protected void messageStandard
    (Dispatcher context
    ,Message message
    )
  {
    
    if (message.getType()!=InitializeMessage.TYPE
        && !permitted()
        )
    { 
      if (debug)
      { log.fine(getLogPrefix(context)+"redirecting- not permitted");
      }
      try
      { setupRedirect((ServiceContext) context);
      }
      catch (ServletException x)
      { x.printStackTrace();
      }
    }
    else
    { super.messageStandard(context,message);
    }
  }
  

  private boolean permitted()
  {
    AuthSession session=sessionChannel.get();
    if (session!=null)
    { 
      return session.isAuthenticated();
    }
    return false;
  }
  
}



