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
import spiralcraft.security.auth.LoginEntry;
import spiralcraft.security.auth.TestAuthenticator;
import spiralcraft.servlet.autofilter.spi.FocusFilter;

import spiralcraft.codec.text.Base64Codec;
import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.reflect.BeanFocus;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;

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
  private static final String AUTH_SESSION_COOKIE_ATTRIBUTE
    ="spiralcraft.servlet.autofilter.SecurityFilter.activeCookie";
  
  private ThreadLocalChannel<AuthSession> authSessionChannel;
  private final ThreadLocal<SecurityFilterContext> contextLocal
    =new ThreadLocal<SecurityFilterContext>();
  
  private String attributeName;  
  private Authenticator authenticator;
  private String cookieName="login";
  private String qualifiedCookieName;
  private String cookieDomain;
  private String cookiePath="/";
  private int minutesToPersist;
  private boolean requireValidCookie;
  private boolean disableLoginCookies;

  private boolean preAuthenticate;
  private String ticketAuthModuleName="local";
  private boolean qualifyCookieName=true;
  
  public void setTicketAuthModuleName(String ticketAuthModuleName)
  { this.ticketAuthModuleName=ticketAuthModuleName;
  }
  
  /**
   * A limit on the the amount of time a persistent login is valid. 
   */
  public void setMinutesToPersist(int minutesToPersist)
  { this.minutesToPersist=minutesToPersist;
  }
  
  /**
   * Disables the use of login cookies
   * 
   * @param disableLoginCookie
   */
  public void setDisableLoginCookies(boolean disableLoginCookies)
  { this.disableLoginCookies=disableLoginCookies;
  } 
  
  /**
   * Attempt to authenticate the session as soon as it is created to
   *   enable automatic login based on contextual credentials 
   * 
   * @param preAuthenticate
   */
  public void setPreAuthenticate(boolean preAuthenticate)
  { this.preAuthenticate=true;
  }
  
  /**
   * <p>Predicates maintaining logged-in state on the presence of a valid 
   *   login cookie. Setting this to true enables persistent login cookies.
   * </p>
   * 
   * <p>Makes all logins persistent regardless of the setting of 
   *   LoginEntry.persistent (aka "Remember Me").
   * </p>
   * 
   * <p>When cookieDomain is used, allows another server in the domain to
   *   effectively logout the user on all servers that sare the same cookie.
   * </p>
   *  
   * 
   * @param requireValidCookie Whether a valid cookie is required to maintain
   *   logged-in state. 
   */
  public void setRequireValidCookie(boolean requireValidCookie)
  { this.requireValidCookie=requireValidCookie;
  }
  
  /**
   * Indicates whether the requireValidCookie property is set.
   * 
   * @see setRequireValidCookie
   * @return
   */
  public boolean getRequireValidCookie()
  { return requireValidCookie;
  }
  
  /**
   * @param authenticator The authenticator which will be used to validate the
   *    login credentials.
   */
  public void setAuthenticator(Authenticator authenticator)
  { this.authenticator=authenticator;
  }

  /**
   * Specify the cookie name for persistent logins. Defaults to "login"
   * 
   * @param cookieName
   */
  public void setCookieName(String cookieName)
  { this.cookieName=cookieName;
  }

  /**
   * Specify whether the realm name should be prepended to the cookie name
   *   to differentiate realms on the same host
   * 
   * @param cookieName
   */
  public void setQualifyCookieName(Boolean qualifyCookieName)
  { this.qualifyCookieName=qualifyCookieName;
  }
  
  /**
   * Specify the RFC2109 cookie domain to use the cookie across multiple
   *   servers in the domain
   *   
   * @param cookieDomain
   */
  public void setCookieDomain(String cookieDomain)
  { 
    if (cookieDomain!=null && !cookieDomain.startsWith("."))
    { 
      throw new IllegalArgumentException
        ("Cookie domain must start with a '.'");
    }
    else if (cookieDomain.equals("."))
    { this.cookieDomain=null;
    }
    else
    { this.cookieDomain=cookieDomain;
    }
  }
  
  /**
   * Specify the absolute URI path to use for the cookie. Defaults to "/".
   * 
   * @param cookiePath
   */
  public void setCookiePath(String cookiePath)
  { this.cookiePath=cookiePath;
  }
  
  /**
   * The cookie name for persistent logins
   * 
   * @return
   */
  public String getCookieName()
  { return cookieName;
  }


  
  /**
   * Read the cookie into the LoginEntry
   * 
   * @param entry
   * @return
   */
  public boolean readLoginCookie(LoginEntry entry)
  {
    if (!usingCookies())
    { return false;
    }
    
    if (contextLocal.get().logoutPending)
    {
      if (debug)
      { log.fine("Logout pending, skipping cookie check");
      }
      return false;
    }
    
    
    Cookie loginCookie=contextLocal.get().getLoginCookie();      
      
    if (loginCookie!=null)
    { 
      if (debug)
      { 
        log.fine
          ("Found a login cookie "
          +loginCookie.getName()+" "
          +loginCookie.getValue()
          );
      }
      return readLoginCookie(entry,loginCookie);
    }
    
    if (debug)
    { log.fine("No login cookie");
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
    // XXX We need to read a non-secret Challenge value,
    //   perhaps by using the expiry time at a minimum, to ensure the
    //   uniqueness of each ticket.
    
    VariableMap map=VariableMap.fromUrlEncodedString(cookie.getValue());
    String username=map.getFirst("username");
    String ticketBase64=map.getFirst("ticket");
    if (username!=null && ticketBase64!=null)
    {

      byte[] ticket=Base64Codec.decodeBytes(ticketBase64);
      entry.reset();
      entry.setUsername(username);
      entry.setSaltedDigest(ticket);
      if (debug)
      { log.fine("Read login info from cookie for user "+username);
      }
      entry.update();
      return true;
    }
    else
    { 
      if (debug)
      { 
        log.fine
          ("Login cookie didn't contain required data: "
          +cookie.getName()+"="+cookie.getValue()
          );
      }
      return false;
    }
  }  

  private boolean usingCookies()
  { return !disableLoginCookies || requireValidCookie; 
  }
  
  /**
   * <p>Called in the request half of the filter when we get a ticket, and 
   *   we're already authenticated. Make sure it is the same ticket we gave out 
   *   when we authenticated an old ticket for this login session.
   * </p>
   * 
   */
  private void revalidateCookieLogin()
  { 
    Cookie loginCookie=contextLocal.get().getLoginCookie();      
    
    if (loginCookie!=null)
    { 
      // This is set to the base64 encoded form of the "remember" token written
      //   after a successful login
      String sessionCookie=
        authSessionChannel.get()
              .getAttribute(AUTH_SESSION_COOKIE_ATTRIBUTE);

      if (debug)
      { 
        log.fine
          ("revalidating login cookie "
          +loginCookie.getName()
          +loginCookie.getValue()
          );
      }
      
      
      
      if (!loginCookie.getValue().equals
            (sessionCookie
            )
         )
      { 
        
        authSessionChannel.get().clearCredentials();
        if (debug)
        {
          log.fine
            ("clearing credentials because cookie doesn't match: "
              +loginCookie.getValue()
              +" != "
              +sessionCookie
              
            );
            
        }
      }
      
    }
    else
    { 
      if (debug)
      { log.fine("clearing credentials because no login cookie");
      }
      authSessionChannel.get().clearCredentials();
    }
    
    
    
  }
  
  public void writeLoginCookie(LoginEntry entry)
  {
    // XXX We need to re-hash the password digest with a non-secret Challenge
    //   to provide a basis for ticket forgery prevention, perhaps by using
    //   the expiry time at a minimum, to ensure the uniqueness of each
    //   Ticket
           
    if (!usingCookies())
    { return;
    }
    
    VariableMap map=new VariableMap();
    String username=authSessionChannel.get().getPrincipal().getName();
    String password=entry.getPasswordCleartext();
    byte[] digest=entry.getSaltedDigest();
    if (username!=null && (password!=null || digest!=null))
    {
      byte[] ticket=digest;
      if (password!=null)
      { 
        ticket=authSessionChannel.get()
          .saltedDigest(username.toLowerCase()+password);
      }
      map.add("username", username);
      map.add("ticket", Base64Codec.encodeBytes(ticket));
      String data=map.generateEncodedForm();
      
      authSessionChannel.get().setAttribute
        (AUTH_SESSION_COOKIE_ATTRIBUTE,data);
      

      
      Cookie cookie=new Cookie(qualifiedCookieName,data);
      
      // Compute seconds, or session level persistence
      cookie.setMaxAge(minutesToPersist>0?minutesToPersist*60:-1);
      if (cookieDomain!=null)
      { cookie.setDomain(cookieDomain);
      }
      if (cookiePath!=null)
      { cookie.setPath(cookiePath);
      }
      writeLoginCookie(cookie);
      if (debug)
      { log.fine("Wrote a login cookie for for user "+username);
      }
    }
    else
    { 
      if (debug)
      { 
        log.fine
          ("Some credentials were unspecified for user '"
          +username+"'- no login cookie created"
          );
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
    throws ContextualException
  { 
    if (authenticator==null)
    { authenticator=new TestAuthenticator();
    }
    this.attributeName=this.getPath().format("/")
      +"!spiralcraft.security.AuthSession";
    
    authSessionChannel
      =new ThreadLocalChannel<AuthSession>
        (BeanReflector.<AuthSession>getInstance(AuthSession.class));
    
    SimpleFocus<AuthSession> authSessionFocus
      =new SimpleFocus<AuthSession>(parentFocus,authSessionChannel);
    authSessionFocus.addFacet(new BeanFocus<SecurityFilter>(this));
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
      // Re-check in synchronized block
      // To avoid race condition of 2 threads associated with the same
      //   session where one overwrites the in-use auth session with
      //   an empty one.
      boolean isNew=false;
      synchronized (session)
      {
        if (qualifiedCookieName==null
            && qualifyCookieName
            && authenticator.getRealmName()!=null
           )
        { qualifiedCookieName=authenticator.getRealmName()+"."+cookieName;
        }
        else
        { qualifiedCookieName=cookieName;
        }
        
        authSession
          =(AuthSession) session.getAttribute(attributeName);      
      
        if (authSession==null)
        { 
          if (debug)
          { 
            log.fine
              ("Creating a new AuthSession for HTTP Session "+session.getId());
          }
          authSession=authenticator.createSession();
          session.setAttribute(attributeName,authSession);
          isNew=true;
        }
        else
        {
          if (debug)
          { 
            log.fine
              ("Successfully avoided race condition for " +
              "HttpSession->AuthSession for session "+session.getId()
              );
          }
        }
      }
      if (isNew)
      {
        if (preAuthenticate)
        { authSession.authenticate();
        }
      }
      
    }
    
    authSession.refresh();
    authSessionChannel.push(authSession);
    
    contextLocal.set(new SecurityFilterContext(request,response,qualifiedCookieName));
    
    
    if (requireValidCookie 
        && authSession.isAuthenticated(ticketAuthModuleName)
        )
    { 
      
      // XXX Note, we now have multi-provider login.
      //   
      
      // Make sure we check the current cookie before allowing a
      //   previously authenticated session to continue
      revalidateCookieLogin();
      
      
    }
    
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
    if (debug)
    { 
      log.fine
        ("Logging out "+authSessionChannel.get().getPrincipal());
    }
    
    authSessionChannel.get().logout();
    // Delete the login cookie
    
    Cookie cookie=new Cookie(qualifiedCookieName,"");
    if (cookieDomain!=null)
    { cookie.setDomain(cookieDomain);
    }
    if (cookiePath!=null)
    { cookie.setPath(cookiePath);
    }
    writeLoginCookie(cookie);
    contextLocal.get().logoutPending=true;
  }  

}

class SecurityFilterContext
{
  public final HttpServletRequest request;
  public final HttpServletResponse response;
  private final String cookieName;
  
  public boolean logoutPending;
  private Cookie loginCookie;
  private volatile boolean checkedCookies;
  
  public SecurityFilterContext
    (HttpServletRequest request,HttpServletResponse response,String cookieName)
  { 
    this.request=request;
    this.response=response;
    this.cookieName=cookieName;
  }
  
  public synchronized Cookie getLoginCookie()
  {
    if (!checkedCookies)
    { 
      checkedCookies=true;
      readLoginCookie();
    }
    
    return loginCookie;
  }
  
  
  /**
   * @return The login Cookie, if present, from the http request
   */
  private void readLoginCookie()
  {
      
    // Check for a cookie
    Cookie[] cookies=request.getCookies();
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
  }  
}

