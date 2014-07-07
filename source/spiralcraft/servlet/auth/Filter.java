//
//Copyright (c) 2012 Michael Toth
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
package spiralcraft.servlet.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.Level;
import spiralcraft.servlet.autofilter.spi.FocusFilter;

/**
 * Exposes authenticator functionality and state for use by web applications
 * 
 * @author mike
 *
 */
public class Filter
  extends FocusFilter<Session>
{

  protected ThreadLocalChannel<Session> channel;
  protected Channel<HttpServletRequest> requestChannel;
  private boolean invalidateOnLogout=false;
        
  { setUsesRequest(true);
  }

  
  public void setInvalidateOnLogout(boolean invalidateOnLogout)
  { this.invalidateOnLogout=invalidateOnLogout;
  }
  


  
  @Override
  public Focus<?> bindExports(Focus<?> focus)
    throws BindException
  { 
    requestChannel=LangUtil.assertChannel(HttpServletRequest.class,focus);
    return focus;
  }
  

  

  
  @Override
  protected Session newPrivateSessionState(HttpServletRequest request)
  { return new Session();
  }
    
  /**
   * Called by specific authentication code to apply the specified
   *   auth credentials to the current session, resetting any existing 
   *   credentials for this auth module.
   * 
   * @param systemId
   * @param token
   */
  public void applyCredentials(String systemId,String token)
  { 
    Session session
      =this.<Session>getPrivateSessionState
        (requestChannel.get(),true);    
    session.applyCredentials(systemId,token);
  }
  
  
  public void abortAuthSequence(String problem)
  {
    Session session
      =this.<Session>getPrivateSessionState
        (requestChannel.get(),true);
    
    session.abortAuthSequence(problem);
  }

  public void logout()
  {
    Session session
      =this.<Session>getPrivateSessionState
        (requestChannel.get(),true);
    if (session==null || !session.isTokenValid())
    { return;
    }

    try
    { 
      if (invalidateOnLogout)
      { session.invalidate();
      }
      else
      { session.clear();
      }
    }
    catch (IOException x)
    { log.log(Level.WARNING,"Error on logout",x);
    }
  }
  
  public void clearSession()
  { channel.get().clear();
  }
  
  /**
   * Called -once- to create the Focus
   */
  @Override
  protected Focus<Session> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 


    channel
      =new ThreadLocalChannel<Session>
        (BeanReflector.<Session>getInstance(Session.class));
    return parentFocus.chain(channel);
  }
  
  
  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
    throws BindException,ServletException
  {
    Session session
      =this.<Session>getPrivateSessionState(request,false);

    
    channel.push(session);
    if (debug)
    { 
      log.debug
        ("Credentials: "
        +session+"("+channel.getReflector().getTypeURI()+")"
        );
    }
    // TODO: Make sure session is active here
    if (session!=null)
    {
      checkSessionValidity(session);
      
    }
    
  }

  
  private void checkSessionValidity(Session session)
  { 
    // Check expire time for access token and trigger a re-auth sequence
    //   somehow
  }
  

  
  @Override
  protected void popSubject(HttpServletRequest request)
  { channel.pop();
  }  
  

}
