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

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.Authenticator;
import spiralcraft.security.auth.Credential;
import spiralcraft.security.auth.TestAuthenticator;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.servlet.security.BasicHttpAdapter;
import spiralcraft.servlet.security.HttpAdapter;

/**
 * <P>Requires HTTP based authentication using an application supplied 
 *   Authenticator implementation.
 * 
 * <P>By default, in order to mitigate the effects of misconfiguration
 *  this filter is global (applies to subdirectories), overridable,
 *  and is not additive (the HTTP protocol only allows one active
 *  set of credentials at a time)
 * 
 * @author mike
 *
 */
public class AuthenticateFilter
    extends AutoFilter
{

  private String realm;
  private Authenticator authenticator;
  private HttpAdapter httpAdapter;
  private String sessionName;
  private boolean useSession;
    
  
  { 
    setOverridable(true);
    setAdditive(false);
    setGlobal(true);
  }

  
  public void setUseSession(boolean val)
  { this.useSession=val;
  }
  
  @Override
  public void init(FilterConfig config)
    throws ServletException
  { 
    super.init(config);
    
    if (httpAdapter==null)
    { httpAdapter=new BasicHttpAdapter();
    }
    
    if (authenticator==null)
    { authenticator=new TestAuthenticator();
    }
    
    realm=authenticator.getRealmName();
    httpAdapter.setRealm(realm);
    sessionName="spiralcraft.security.auth."+realm;

  }
  
  /**
   * @param authenticator The authenticator which will be used to validate the
   *    userId and password
   */
  public void setAuthenticator(Authenticator authenticator)
  { this.authenticator=authenticator;
  }
  
  @Override
  public void doFilter
    (ServletRequest request
    ,ServletResponse response
    ,FilterChain chain
    )
    throws IOException,ServletException
  {
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    HttpServletResponse httpResponse=(HttpServletResponse) response;

    try
    {
      Focus<?> focus=FocusFilter.getFocusChain(httpRequest);
      authenticator.bind(focus);
    }
    catch (BindException x)
    { throw new ServletException("Error binding uthenticator",x);
    }
        
    if (isAuthenticated(httpRequest,httpResponse))
    { chain.doFilter(request,response);
    }
  }
  
  public boolean isAuthenticated
    (HttpServletRequest httpRequest
    ,HttpServletResponse httpResponse
    )
    throws IOException,ServletException
  {
    
    AuthSession session=null;
    if (useSession)
    { 
      HttpSession httpSession
        =httpRequest.getSession(useSession);
      
      // Re-check in synchronized block
      // To avoid race condition of 2 threads associated with the same
      //   session where one overwrites the in-use auth session with
      //   an empty one.      
      session=(AuthSession) httpSession.getAttribute(sessionName);
      
      if (session==null)
      {
        synchronized (httpSession)
        {
          session=(AuthSession) httpSession.getAttribute(sessionName);
          if (session==null)
          {
            session=authenticator.createSession();
            httpSession.setAttribute(sessionName,session);
            if (debug)
            { 
              log.fine
                ("Created new AuthSession in HttpSession "
                +httpSession.getId()
                );
            }
          }
          else
          {
            if (debug)
            { 
              log.fine
                ("Avoided race condition for creation of AuthSession in"
                +"HttpSession "+httpSession.getId()
                );
            }
          }
        }
      }
      
    }
    
    if (session!=null && session.isAuthenticated())
    { 
      // If useSession==true, we need to trust the HttpSession
      return true;
    }
    
    Credential<?>[] credentials
      =httpAdapter.readAuthorization(httpRequest);
    
    if (credentials==null)
    { 
      httpAdapter.writeChallenge(httpResponse);
      return false;
    }
    else
    {        
      // Try to authenticate the credentials
      if (session==null)
      { 
        // Only happens if useSession=false
        session=authenticator.createSession();
        if (debug)
        { log.fine("Created new AuthSession- not using HttpSession");
        }

      }
      if (session==null)
      { 
        log.warning
          ("AuthenticateFilter: authenticator failed to create session");
        return false;
      }
      
      session.addCredentials(credentials);
      if (session.authenticate())
      { 
        if (debug)
        { log.fine("Authentication successful");
        }
        return true;
      }
      else
      { 
        if (debug)
        { log.fine("Authentication failed");
        }
        httpAdapter.writeChallenge(httpResponse);
        return false;
      }
        
    }
  }

}
