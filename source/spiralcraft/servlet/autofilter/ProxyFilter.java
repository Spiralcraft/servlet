//
//Copyright (c) 2009,2009 Michael Toth
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
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;



import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.text.ParseException;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.ContextDictionary;
// import spiralcraft.util.Path;
// import spiralcraft.util.PathPattern;
import spiralcraft.vfs.StreamUtil;

public class ProxyFilter
    extends AutoFilter
{

  private String proxyURL;
  private String proxyQuery;
  private boolean useRequestPath=false;
  private boolean absolute;
  private boolean bound;
  private ParameterBinding<?>[] queryBindings;
  private String[] permittedAuthorities;
  private String proxyURLParameter;
  
  
//  private PathPattern pattern;

  /**
   * A fixed base URL to which incoming requests will be routed. 
   */
  public void setProxyURL(String url)
  { 
    this.proxyURL=url;
    try
    { 
      this.proxyURL=ContextDictionary.substitute(url);
      URI uri=URI.create(this.proxyURL);
      absolute=uri.isAbsolute();
      proxyQuery=uri.getRawQuery();
    }
    catch (ParseException x)
    { throw new IllegalArgumentException(x);
    }
    
    
  }
  
  /**
   * The URL query parameter which contains the encoded destination URL
   * 
   * @param proxyURLParameter
   */
  public void setProxyURLParameter(String proxyURLParameter)
  { this.proxyURLParameter=proxyURLParameter;
  }
  
  /**
   * A list of authorities (ie. host or host:port ) that will be permitted
   *   for forwarding.
   * 
   * @param authorities
   */
  public void setPermittedAuthorities(String[] permittedAuthorities)
  { this.permittedAuthorities=permittedAuthorities;
  }
  
  public void setQueryBindings(ParameterBinding<?>[] queryBindings)
  { this.queryBindings=queryBindings;
  }
  
//  /**
//   * Adds the entire path of the incoming request to the specified proxy URL.
//   *    
//   * @param useRequestURI
//   */
//  public void setUseRequestPath(boolean useRequestPath)
//  { this.useRequestURI=useRequestPath;
//  }
  
//  public void setPattern(PathPattern pattern)
//  { this.pattern=pattern;
//  }
  
  public void bind(Focus<?> focus)
    throws BindException
  {

    if (queryBindings!=null)
    {
      if (focus!=null)
      {
        for (ParameterBinding<?> binding: queryBindings)
        { binding.bind(focus);
        }
      }
      else
      { throw new BindException("No Focus");
      }
    }    
    if (proxyURLParameter!=null && permittedAuthorities==null)
    {
      throw new BindException
        ("permittedAuthorities must be specified when using proxyURLParameter" +
          " to avoid unrestricted proxy"
        );
    }
    bound=true;
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
      { throw new ServletException("Error binding ProxyFilter",x);
      }
    }

    
//    if (pattern==null 
//        || !pattern.matches(new Path(httpRequest.getRequestURI(),'/'))
//       )
//    { 
//      chain.doFilter(request,response);
//      return;
//    }
    
    String url;
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    
    boolean absolute=this.absolute;
    String proxyURL=this.proxyURL;
    String queryString;

    String requestPath
      =httpRequest.getRequestURI().substring(1);      
    
    if (proxyURLParameter!=null)
    { 
      proxyURL=request.getParameter(proxyURLParameter);
      if (proxyURL==null)
      { 
        httpResponse.sendError(404,"No proxy url");
        return;
      }
      
      absolute=URI.create(proxyURL).isAbsolute();
      
      // Query is already contained in proxyURL
      queryString=null;
      
    }
    else
    { 
      queryString=httpRequest.getQueryString();

    }
    
    
    
    if (proxyQuery!=null)
    { queryString=queryString!=null?queryString+"&"+proxyQuery:proxyQuery;
    }
    
    if (queryBindings!=null)
    {
      
      VariableMap map;
      if (queryString!=null && queryString.length()>0)
      { map=VariableMap.fromUrlEncodedString(queryString);
      }
      else
      { map=new VariableMap();
      }
      
      for (ParameterBinding<?> binding: queryBindings)
      { binding.read(map);
      }
    
      for (ParameterBinding<?> binding: queryBindings)
      { binding.publish(map);
      }
      
      queryString=map.generateEncodedForm();
    }
    
    if (debug && queryString!=null)
    { log.fine("proxy query="+queryString);
    }
    
    if (!absolute)
    { 
      URI requestURL
        =URI.create(httpRequest.getRequestURL().toString());
      if (debug)
      { log.fine(requestURL.toString());
      }
      
      url
        =requestURL.resolve
          (proxyURL
            +(useRequestPath?"/"+requestPath:"")
            +(queryString!=null
              ?"?"+queryString
              :""
             )
          ).toString();
      
      if (debug)
      { log.fine(url);
      }
      
    }
    else
    {
      
      url=
        proxyURL
        +(useRequestPath?"/"+requestPath:"")
        +(queryString!=null
         ?"?"+queryString
         :""
         );

      if (debug)
      { log.fine(url);
      }
    }
    
    if (permittedAuthorities!=null)
    {
      String authority=URI.create(url).getAuthority();
      if (!ArrayUtil.contains(permittedAuthorities,authority))
      { 
        httpResponse.sendError(403,"Forbidden proxy URL "+url);
        return;
      }
    }
    
    doProxy(httpRequest,httpResponse,url);
    
    
  }
  
  private void doProxy
    (HttpServletRequest httpRequest
    ,HttpServletResponse httpResponse
    ,String url
    )
    throws IOException,ServletException
  {
    if (debug)
    { log.debug("Connecting to "+url);
    }
    URLConnection connection=new URL(url).openConnection();
    connection.setAllowUserInteraction(false);
    connection.setConnectTimeout(15000);
    connection.setUseCaches(false);
    connection.setReadTimeout(5000);
    
    if (httpRequest.getMethod().equals("POST"))
    {
      setupRequest(httpRequest,connection);
      
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.connect();
      
      if (debug)
      { log.debug("Connected to "+url);
      }
      relayRequestContent(httpRequest,connection);
      
      setupResponse(httpResponse,connection);
      
      relayResponse(httpResponse,connection);
      if (debug)
      { log.debug("Completed "+url);
      }
    }
    else if (httpRequest.getMethod().equals("GET"))
    {
      setupRequest(httpRequest,connection);
      connection.setDoInput(true);
      connection.setDoOutput(false);
      connection.connect();
      if (debug)
      { log.debug("Connected to "+url);
      }
      setupResponse(httpResponse,connection);
      relayResponse(httpResponse,connection);
      if (debug)
      { log.debug("Completed "+url);
      }
    }
    else
    { throw new ServletException("Can't proxy a "+httpRequest.getMethod());
    }
  }

  private void setupRequest
    (HttpServletRequest httpRequest
    ,URLConnection connection
    )
  {
    connection.setRequestProperty("Connection","close");
    if (httpRequest.getContentType()!=null)
    { connection.setRequestProperty("ContentType",httpRequest.getContentType());
    }
    if (httpRequest.getContentLength()>0)
    { 
      connection.setRequestProperty
        ("ContentLength",Integer.toString(httpRequest.getContentLength()));
      
    }
  }

  private void relayRequestContent
    (HttpServletRequest httpRequest
    ,URLConnection connection
    )
    throws IOException
  {
    OutputStream out=connection.getOutputStream();
    try
    { 
      StreamUtil.copyRaw
        (httpRequest.getInputStream(),out,8192,httpRequest.getContentLength());
    }
    finally
    { out.close();
    }
  }
  
  private void setupResponse
    (HttpServletResponse httpResponse
    ,URLConnection connection
    )
  {
    String contentType=connection.getContentType();
    int contentLength=connection.getContentLength();
        
    if (contentType!=null)
    { httpResponse.setContentType(contentType);
    }
    if (contentLength>-1)
    { httpResponse.setContentLength(contentLength);
    }
    
  }

  private void relayResponse
    (HttpServletResponse httpResponse
    ,URLConnection connection
    )
    throws IOException
  {

    int contentLength=connection.getContentLength();
    InputStream in=connection.getInputStream();
    try
    { 
      StreamUtil.copyRaw
        (in,httpResponse.getOutputStream(),8192,contentLength);
    }
    finally
    { in.close();
    }      
    httpResponse.flushBuffer();
    
  }
  
  public String getFilterType()
  { return "proxy";
  }

}