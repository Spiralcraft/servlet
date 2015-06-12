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
import java.net.URISyntaxException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>Redirects incoming requests that match an optional prefix
 * </p>
 * 
 * <p>If a prefix is not provided, the specified redirectURL represents a
 *   fixed location to redirect all traffic to, irrespective of the original 
 *   host, or path. If the redirectURL contains no scheme or authority 
 *   components (i.e. is a local redirect), the query portion of the incoming
 *   request will be preserved
 * </p>
 * 
 * <p>If a prefix is provided, only incoming requests matching the prefix
 *   will be redirected. The matching portions of the prefix will be replaced
 *   with their conterpart components in the redirectURL. 
 * </p>
 * 
 * @author mike
 *
 */
public class RedirectFilter
    extends AutoFilter
{

  private String redirectURL;
  private String targetScheme;
  private String targetAuthority;
  private String targetPath;
  private String targetQuery;
  private boolean absolute;
  private URI prefix;
  private boolean permanent=false;
  private String matchScheme;
  private String matchAuthority;
  private String matchPath;
  private boolean changeParts;

  public void setRedirectURL(String url)
  { 
    URI uri=URI.create(url);
    absolute=uri.isAbsolute();
    this.redirectURL=url;
    
    this.targetScheme=uri.getScheme();
    this.targetAuthority=uri.getAuthority();
    this.targetPath=uri.getPath();
    this.targetQuery=uri.getQuery();
    
  }
  
  /**
   * <p>Supply the prefix to match. The scheme, authority and path
   *   components are individually tested; components with null values are
   *   not used in the comparison.
   * </p>
   * 
   * <p>If a prefix matches, the matching portion will be substituted with
   *   the appropriate components in the redirectURL. 
   * </p> 
   * 
   * @param prefix
   */
  public void setPrefix(URI prefix)
  { 
    this.prefix=prefix;
    this.matchScheme=prefix!=null?prefix.getScheme():null;
    this.matchAuthority=prefix!=null?prefix.getAuthority():null;
    this.matchPath=prefix!=null?prefix.getPath():null;
    if (prefix!=null)
    { changeParts=true;
    }
    
  }
  
  public void setChangeHost(String host)
  {
    this.targetAuthority=host;
    changeParts=true;
  }
  
  /**
   * <p>Specify whether a permanent redirect (using HTTP response code 301) 
   *   should be instead of a temporary redirect (which uses HTTP response code
   *   302).
   * </p>
   * 
   * @param permanent
   */
  public void setPermanent(boolean permanent)
  { this.permanent=permanent;
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
    
    if (debug)
    { log.debug("Redirect filter in "+getPath()+" processing "+httpRequest.getRequestURI());
    }
    
    URI requestURI
      =URI.create(((HttpServletRequest) request).getRequestURL().toString());
    String url=redirectURL;
    
    if (changeParts)
    {

           
      if (matchScheme!=null 
          && !matchScheme.equals(requestURI.getScheme()))
      { 
        if (debug)
        { log.debug("Skipping, no scheme match");
        }
        chain.doFilter(request,response);
        return;
      }
      
      if (matchAuthority!=null 
          && !matchAuthority.equals(requestURI.getAuthority()))
      { 
        if (debug)
        { log.debug("Skipping, no authority match");
        }
        chain.doFilter(request,response);
        return;
      }

      String absMatchPath
        =(matchPath==null || matchPath.isEmpty())
        ?null
        :(matchPath.startsWith("/")
          ?matchPath
          :(getPath()+"/"+matchPath)
        );
      
      if (absMatchPath!=null 
          && (requestURI.getPath()==null
              || !requestURI.getPath().startsWith(absMatchPath)
             )
         )
      { 
        if (debug)
        { log.debug("Skipping, no match to path "+absMatchPath);
        }
        chain.doFilter(request,response);
        return;
      }
      
      String appendPath=requestURI.getPath();
      if (absMatchPath!=null)
      { appendPath=requestURI.getPath().substring(absMatchPath.length());
      }
      
      
      String newScheme
        =targetScheme!=null
          ?targetScheme
          :matchScheme!=null
          ?matchScheme
          :requestURI.getScheme()
          ;

      String newAuthority
        =targetAuthority!=null
          ?targetAuthority
          :matchAuthority;
      
      if (newScheme!=null && newAuthority==null)
      { newAuthority=requestURI.getAuthority();
      }
      if (newAuthority!=null && newScheme==null)
      { newScheme=requestURI.getScheme();
      }
      
      String newPath=targetPath!=null?targetPath+appendPath:appendPath;
      String newQuery=targetQuery!=null
        ?targetQuery+requestURI.getQuery():requestURI.getQuery();
      
      
      try
      { 
        URI destURI=new URI(newScheme,newAuthority,newPath,newQuery,null);
        url=destURI.toString();
      }
      catch (URISyntaxException x)
      { throw new ServletException("Error creating redirect URI ",x);
      }
    }
    else
    {
      if (!absolute)
      { 
        // Local redirect should preserve query part
        url
          =requestURI.resolve
            (redirectURL
              +(requestURI.getRawQuery()!=null
                ?"?"+requestURI.getRawQuery()
                :""
               )
            ).toString();
      }
    }
    
    
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    if (debug)
    { log.debug("Redirecting to "+url);
    }
    if (!permanent)
    { httpResponse.sendRedirect(httpResponse.encodeRedirectURL(url));
    }
    else
    { 
      httpResponse.setStatus(301);
      httpResponse.setHeader("Location",url);
    }
  }

  public String getFilterType()
  { return "redirect";
  }

}