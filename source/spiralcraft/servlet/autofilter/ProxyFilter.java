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



import spiralcraft.text.ParseException;
import spiralcraft.text.html.URLEncoder;
import spiralcraft.util.ContextDictionary;
// import spiralcraft.util.Path;
// import spiralcraft.util.PathPattern;
import spiralcraft.vfs.StreamUtil;

public class ProxyFilter
    extends AutoFilter
{

  private String proxyURL;
  private boolean useRequestURI=false;
  private boolean absolute;
//  private PathPattern pattern;

  public void setProxyURL(String url)
  { 
    this.proxyURL=url;
    try
    { 
      this.proxyURL=ContextDictionary.substitute(url);
      URI uri=URI.create(this.proxyURL);
      absolute=uri.isAbsolute();
    }
    catch (ParseException x)
    { throw new IllegalArgumentException(x);
    }
    
    
  }
  
//  public void setPattern(PathPattern pattern)
//  { this.pattern=pattern;
//  }
  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException,ServletException
  {
    
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    
//    if (pattern==null 
//        || !pattern.matches(new Path(httpRequest.getRequestURI(),'/'))
//       )
//    { 
//      chain.doFilter(request,response);
//      return;
//    }
    
    String url;
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    
    String encodedRequestURI
      =URLEncoder.encode(httpRequest.getRequestURI()).substring(1);      
    
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
            +(useRequestURI?"/"+encodedRequestURI:"")
            +(requestURL.getQuery()!=null
              ?"?"+requestURL.getQuery()
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
        +(useRequestURI?"/"+encodedRequestURI:"")
        +(httpRequest.getQueryString()!=null
         ?"?"+httpRequest.getQueryString()
         :""
         );

      if (debug)
      { log.fine(url);
      }
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
      
      
      relayRequestContent(httpRequest,connection);
      
      setupResponse(httpResponse,connection);
      
      relayResponse(httpResponse,connection);
    }
    else if (httpRequest.getMethod().equals("GET"))
    {
      setupRequest(httpRequest,connection);
      connection.setDoInput(true);
      connection.setDoOutput(false);
      connection.connect();
      setupResponse(httpResponse,connection);
      relayResponse(httpResponse,connection);
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
    connection.setRequestProperty("ContentType",httpRequest.getContentType());
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

  @Override
  public void setGeneralInstance(AutoFilter parentInstance)
  {

  }

  @Override
  public Class<? extends AutoFilter> getCommonType()
  { return RedirectFilter.class;
  }
}