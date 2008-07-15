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
package spiralcraft.servlet.autofilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.Authenticator;
import spiralcraft.security.auth.TestAuthenticator;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.lang.BeanFocus;
import spiralcraft.lang.BindException;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;


/**
 * <P>Provides an application with access to stateful security components
 *   which provide user authentication services and login/id state.
 * </P>
 * 
 * <P>Publishes the spiralcraft.security.auth.AuthSession reference into the
 *   Focus chain, along with a self reference to provide access to 
 * </P>
 * 
 * @author mike
 *
 */
public class SecurityFilter
  extends FocusFilter<AuthSession>
{
  
  private ThreadLocalChannel<AuthSession> authSessionChannel;
  private String attributeName;  
  private Authenticator authenticator;
  
  /**
   * @param authenticator The authenticator which will be used to validate the
   *    userId and password
   */
  public void setAuthenticator(Authenticator authenticator)
  { 
    this.authenticator=authenticator;
  }
  
  /**
   * Called -once- to create the Focus
   */
  protected Focus<AuthSession> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    if (authenticator==null)
    { authenticator=new TestAuthenticator();
    }
    this.attributeName=this.getPath().format("/")
      +"!spiralcraft.security.AuthSession";
    
    // XXX Replace with XML binding
    authSessionChannel
      =new ThreadLocalChannel<AuthSession>
        (BeanReflector.<AuthSession>getInstance(AuthSession.class));
    
    CompoundFocus<AuthSession> authSessionFocus
      =new CompoundFocus<AuthSession>(parentFocus,authSessionChannel);
    authSessionFocus.bindFocus
      ("spiralcraft.servlet",new BeanFocus<SecurityFilter>(this));
    authenticator.bind(authSessionFocus);
    return authSessionFocus;
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  {
    authSessionChannel.pop();
    
  }
  
  

  @Override
  protected void pushSubject(HttpServletRequest request) 
    throws BindException
  {
      
    HttpSession session=request.getSession();
    AuthSession authSession
      =(AuthSession) session.getAttribute(attributeName);
      
    if (authSession==null)
    { 
      authSession=authenticator.createSession();
      session.setAttribute(attributeName,authSession);
    }
    authSessionChannel.push(authSession);      
  }
  
  /**
   * Logs out the user by calling logout() on the referenced AuthSession.
   * @return
   */
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
  { authSessionChannel.get().logout();
  }  

}
