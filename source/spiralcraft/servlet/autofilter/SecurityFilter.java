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

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.Authenticator;
import spiralcraft.security.auth.LoginEntry;
import spiralcraft.security.auth.TestAuthenticator;

import spiralcraft.codec.CodecException;
import spiralcraft.codec.text.Base64Codec;
import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.lang.BeanFocus;
import spiralcraft.lang.BindException;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLogger;
import spiralcraft.net.http.VariableMap;


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
  
  private static final ClassLogger log
    =ClassLogger.getInstance(SecurityFilter.class);
  
  private ThreadLocalChannel<AuthSession> authSessionChannel;
  private final ThreadLocal<SecurityFilterContext> contextLocal
    =new ThreadLocal<SecurityFilterContext>();
  
  private String attributeName;  
  private Authenticator authenticator;
  private String cookieName="login";
  private int minutesToPersist;

  /**
   * The amount of time a login should persist. Setting this enables
   *   persistent login cookies.
   */
  public void setMinutesToPersist(int minutesToPersist)
  { this.minutesToPersist=minutesToPersist;
  }
  
  
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

  public boolean readLoginCookie(LoginEntry entry)
  {
    if (minutesToPersist<=0)
    { return false;
    }
    
    Cookie loginCookie=null;
      
    // Check for a cookie
    Cookie[] cookies=contextLocal.get().request.getCookies();
    if (cookies!=null)
    {
      for (Cookie cookie:cookies)
      { 
        if (cookie.getName().equals(cookieName))
        { 
          loginCookie=cookie;
          break;
        }
      }
    }
      
      
    if (loginCookie!=null)
    { return readLoginCookie(entry,loginCookie);
    }
    return false;
  }

  /**
   * <p>Read the login cookie and return whether login data was successfully
   *  read
   * </p>
   * 
   * @param cookie
   * @return Whether login data was successfully read
   */
  private boolean readLoginCookie(LoginEntry entry,Cookie cookie)
  {
    VariableMap map=VariableMap.fromUrlEncodedString(cookie.getValue());
    String username=map.getOne("username");
    String ticketBase64=map.getOne("ticket");
    if (username!=null && ticketBase64!=null)
    {
      entry.setUsername(username);
      try
      { entry.setOpaqueDigest(Base64Codec.decodeBytes(ticketBase64));
      }
      catch (IOException x)
      { 
        x.printStackTrace();
        return false;
      }
      catch (CodecException x)
      {
        x.printStackTrace();
        return false;
      }
      return true;
    }
    else
    { return false;
    }
  }  
  
  public void writeLoginCookie(LoginEntry entry)
  {
    if (minutesToPersist<=0)
    { return;
    }
      
    VariableMap map=new VariableMap();
    String username=entry.getUsername();
    String password=entry.getPasswordCleartext();
    if (username!=null && password!=null)
    {
      byte[] ticket=authSessionChannel.get().opaqueDigest(username+password);
      map.add("username", entry.getUsername());
      try
      { map.add("ticket", Base64Codec.encodeBytes(ticket));
      }
      catch (IOException x)
      {
        x.printStackTrace();
        return;
      }
      catch (CodecException x)
      { 
        x.printStackTrace();
        return;
      }
      String data=map.generateEncodedForm();
      Cookie cookie=new Cookie(cookieName,data);
      cookie.setMaxAge(minutesToPersist*60); // Convert from seconds
      writeLoginCookie(cookie);
    }
    else
    { 
      if (debug)
      { 
        log.fine
          ("Some credentials were unspecified- no login cookie created");
      }
    }
  }
  
  private void writeLoginCookie(Cookie cookie)
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
