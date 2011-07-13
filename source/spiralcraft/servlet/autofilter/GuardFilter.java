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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.Permission;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.text.html.URLDataEncoder;

/**
 * Protects access to a resource, allowing access when a specified
 *   guard Expression<Boolean> returns true, and returning an
 *   alternate result otherwise.
 * 
 * @author mike
 *
 */
public class GuardFilter
    extends AutoFilter
{

  private boolean bound=false;
  private Binding<String> messageX;
  private Binding<Boolean> guardX;
  private Binding<URI> redirectUriX;
  private Binding<Permission[]> permissionsX;
  private Channel<AuthSession> authSessionX;
  private URI loginURI;
  private boolean authenticate;
  
  private int responseCode=501;
  
  {
    setGlobal(true);
    setPattern("*");
  }

  
  public void setResponseCode(int responseCode)
  { this.responseCode=responseCode;
  }
  
  public void setMessageX(Binding<String> messageX)
  { this.messageX=messageX;
  } 
  
  public void setGuardX(Binding<Boolean> guardX)
  { this.guardX=guardX;
  }
  
  public void setRedirectUriX(Binding<URI> redirectUriX)
  { this.redirectUriX=redirectUriX;
  }
  
  public void setPermissionsX(Binding<Permission[]> permissionsX)
  { this.permissionsX=permissionsX;
  }
  
  public void setAuthenticate(boolean authenticate)
  { this.authenticate=authenticate;
  }
  
  public void setLoginURI(URI loginURI)
  { this.loginURI=loginURI;
  }
  
  public void bind(Focus<?> focus)
    throws BindException
  {
    authSessionX=LangUtil.findChannel(AuthSession.class,focus);
    if (messageX!=null)
    { 
      messageX.bind(focus);
      if (debug)
      { messageX.setDebug(debug);
      }
    }
    if (redirectUriX!=null)
    { redirectUriX.bind(focus);
    }
    if (guardX!=null)
    { 
      guardX.bind(focus);
      if (debug)
      { guardX.setDebug(debug);
      }
    }
    if (permissionsX!=null)
    { permissionsX.bind(focus);
    }
    
    if (guardX==null && permissionsX==null && !authenticate)
    {
      throw new BindException
        ("GuardFilter must be configured with one or more of the following "
         +" properties: guardX,permissionX,authenticate"
        );
    }
    bound=true;
  }  
  
  private URI createRedirectURI(URI redirectURI,HttpServletRequest request)
  {
    String referer=request.getRequestURL().toString();
    URI redirect
      =URI.create
        (redirectURI.getPath()+"?referer="+URLDataEncoder.encode(referer));
    return redirect;
  }

  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException,ServletException
  {
    
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    
    if (!bound)
    { 
      try
      { bind(FocusFilter.getFocusChain(httpRequest));
      }
      catch (BindException x)
      { throw new ServletException("Error binding GuardFilter",x);
      }
    }
    
    Boolean val=guardX!=null?guardX.get():null;
    boolean failedAuthentication=false;
    
    if (val==null || Boolean.TRUE.equals(val) && authenticate)
    { 
      
      if (authSessionX==null)
      { val=false;
      }
      else
      {
        AuthSession session=authSessionX.get();
        if (session==null)
        { val=false;
        }
        else
        { 
          if (session.isAuthenticated())
          { val=true;
          }
          else
          { 
            val=false;
            failedAuthentication=true;
          }
        }
      }
    }
    
    if (val==null || Boolean.TRUE.equals(val) && (permissionsX!=null))
    {
      if (authSessionX==null)
      { val=false;
      }
      else
      {
        AuthSession session=authSessionX.get();
        if (session==null)
        { val=false;
        }
        else
        { 
          Permission[] permissions=permissionsX.get();
          if (permissions!=null)
          { val=session.hasPermissions(permissions);
          }
        }
            
      }
    }
        
    
    if (!Boolean.TRUE.equals(val))
    {
      if (debug)
      { log.debug("GuardFilter rejected request");
      }
        
      HttpServletResponse httpResponse=(HttpServletResponse) response;
      

      if (failedAuthentication && loginURI!=null)
      {
        String referer=((HttpServletRequest) request).getRequestURL().toString();
        URI redirectURI
          =URI.create
            (loginURI.getPath()+"?referer="+URLDataEncoder.encode(referer));
        if (debug)
        { log.fine("Setting up redirect to "+redirectURI);
        }
        httpResponse.sendRedirect
          (createRedirectURI(redirectURI,httpRequest).toString());
      }
      else
      {
        URI redirectURI=redirectUriX!=null?redirectUriX.get():null;
        if (redirectURI!=null)
        { 
          if (debug)
          { log.fine("Setting up redirect to "+redirectURI);
          }
          httpResponse.sendRedirect
            (createRedirectURI(redirectURI,httpRequest).toString());
        }
        else if (messageX!=null)
        { 
  
          httpResponse.setStatus(responseCode);
          String message=messageX.get();
          if (message!=null)
          {
            httpResponse.setContentType("text/plain");
            httpResponse.setContentLength(message.length());
            response.getWriter().write(message);
            response.getWriter().flush();
          }
        }
      }
    }
    else
    { 
      
      if (debug)
      { log.debug("GuardFilter passed request");
      }
      chain.doFilter(request,response);
    }
  }

  public String getFilterType()
  { return "guard";
  }

}