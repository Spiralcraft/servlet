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

import java.net.URI;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
 * <p>Provides an application with access to stateful security components
 *   which provide user authentication services and login/id state.
 * </p>
 * 
 * <p>Publishes the spiralcraft.security.auth.AuthSession reference into the
 *   Focus chain, along with a self reference to support HTTP aware commands.
 * </p>
 * 
 * @author mike
 *
 */
public class SecurityFilter
  extends FocusFilter<AuthSession>
{
  public static final URI FOCUS_URI
    =URI.create("class:/spiralcraft/servlet/autofilter/SecurityFilter");
  
  private ThreadLocalChannel<AuthSession> authSessionChannel;
  private final ThreadLocal<SecurityFilterContext> contextLocal
    =new ThreadLocal<SecurityFilterContext>();
  
  private String attributeName;  
  private Authenticator authenticator;
  private String cookieName="login";
  
  /**
   * @param authenticator The authenticator which will be used to validate the
   *    login credentials.
   */
  public void setAuthenticator(Authenticator authenticator)
  { this.authenticator=authenticator;
  }

  /**
   * Specify the cookie name for persistent logins
   * 
   * @param cookieName
   */
  public void setCookieName(String cookieName)
  { this.cookieName=cookieName;
  }

  /**
   * The cookie name for persistent logins
   * 
   * @return
   */
  public String getCookieName()
  { return cookieName;
  }
  
  public void writeLoginCookie(Cookie cookie)
  { contextLocal.get().response.addCookie(cookie);
  }
  
  /**
   * Called -once- to create the Focus
   */
  @Override
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
      (cookieName,new BeanFocus<SecurityFilter>(this));
    authenticator.bind(authSessionFocus);
    return authSessionFocus;
  }
  
  
  

  @Override
  protected void pushSubject
    (HttpServletRequest request
    ,HttpServletResponse response
    ) 
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
    contextLocal.set(new SecurityFilterContext(request,response));
    
  }

  @Override
  protected void popSubject(HttpServletRequest request)
  {
    contextLocal.remove();
    authSessionChannel.pop();
    
  }
  
  /**
   * <p>A command which logs out the user by calling logout() on the referenced
   *   AuthSession.
   * </p>
   * @return The command
   */
  public Command<Void,Void> logoutCommand()
  {     
    return new CommandAdapter<Void,Void>()
      { 
        @Override
        public void run()
        { logout();
        }
      };
  }
    
  private void logout()
  { 
    authSessionChannel.get().logout();
    // Delete the login cookie
    
    writeLoginCookie(new Cookie(cookieName,""));
  }  

}

class SecurityFilterContext
{
  public final HttpServletRequest request;
  public final HttpServletResponse response;
  
  public SecurityFilterContext
    (HttpServletRequest request,HttpServletResponse response)
  { 
    this.request=request;
    this.response=response;
  }
  
}

