//
// Copyright (c) 1998,2005 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.log.ClassLog;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.UnresolvableURIException;

import spiralcraft.vfs.file.FileResource;
import spiralcraft.vfs.url.URLResource;



/**
 * <p>Base class for web servlets, with useful utility methods to simplify
 *  implementations.
 * </p>
 */
public class HttpServlet
  implements Servlet
{

  protected final ClassLog log=
      ClassLog.getInstance(getClass());
  
  public static final String METHOD_GET="GET";
  public static final String METHOD_HEAD="HEAD";
  public static final String METHOD_POST="POST";
  public static final String METHOD_PUT="PUT";
  public static final String METHOD_OPTIONS="OPTIONS";
  public static final String METHOD_TRACE="TRACE";
  public static final String METHOD_DELETE="DELETE";

  private ServletConfig config;
  protected Set<String> recognizedParameters;
  

  @SuppressWarnings("unchecked")
  public void init(ServletConfig config)
    throws ServletException
  { 
    this.config=config;
    
    Enumeration<String> names=config.getInitParameterNames();
    while (names.hasMoreElements())
    {
      String name=names.nextElement();
      if (recognizedParameters!=null 
          && !recognizedParameters.contains(name)
         )
      {
        throw new ServletException
          ("Unrecognized init parameter '"+name+"'. Recognized parameters are "
          +" "+recognizedParameters.toString()
          );
      }
      setInitParameter(name,config.getInitParameter(name));
      
    }    
  }
  
  /**
   * <p>Override to handle initialization parameters. The default
   *   implementation does nothing.
   * </p>
   * 
   * <p>Used in conjunction with the protected 'recognizedParameters' set,
   *   parameters not listed in the set will not reach this method.
   * </p>
   * 
   * 
   * @param name
   * @param value
   * @throws ServletException
   */
  protected void setInitParameter(String name,String value)
    throws ServletException
  { }
  
  public void destroy()
  {
  }
  
  /**
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  protected void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
  }
  
  /**
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  protected void doHead(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  { 
  }

  /**
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  protected void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
  }

  /**
   *   
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */ 
  protected void doPut(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
  }

  /**
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  protected void doOptions(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  { 
  }

  /**
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  protected void doTrace(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
  }

  /**
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  protected void doDelete(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
  }

  /**
   * <p>Provides a default implementation of service(request,response) which
   *   delegates handling to different methods according to the HTTP request
   *   method.
   * </p>
   */
  public void service(ServletRequest request,ServletResponse response)
    throws ServletException,IOException
  {
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    String method=httpRequest.getMethod().intern();

    if (method==METHOD_GET)
    { doGet(httpRequest,httpResponse);
    }
    else if (method==METHOD_HEAD)
    { doHead(httpRequest,httpResponse);
    }
    else if (method==METHOD_POST)
    { doPost(httpRequest,httpResponse);
    }
    else if (method==METHOD_PUT)
    { doPut(httpRequest,httpResponse);
    }
    else if (method==METHOD_OPTIONS)
    { doOptions(httpRequest,httpResponse);
    }
    else if (method==METHOD_TRACE)
    { doTrace(httpRequest,httpResponse);
    }
    else if (method==METHOD_DELETE)
    { doDelete(httpRequest,httpResponse);
    }
    
  }

  public ServletConfig getServletConfig()
  { return config;
  }
  
  public String getServletInfo()
  { return getClass().getName();
  }
  
  protected void throwServletException(String message,Throwable cause)
    throws ServletException
  {
    ServletException x=new ServletException(message);
    if (cause!=null)
    { x.initCause(cause);
    }
    throw x;
  }
  
  /**
   * Return the path within the servlet context of the request. Effectively
   *   concatenates the servletPath+pathInfo
   */
  protected String getContextRelativePath(HttpServletRequest request)
  { 
    String servletPath=request.getServletPath();
    String pathInfo=request.getPathInfo();
    
    String combinedPath=servletPath==null?"":servletPath;
    if (pathInfo!=null)
    { combinedPath=servletPath+pathInfo;
    }
    return combinedPath;
  }
  
  /**
   * <p>Return a spiralcraft.vfs.Resource that provides access to a
   *   resource relative to the ServletContext, whether it exists or not.
   * </p>
   */
  public Resource getResource(String contextRelativePath)
    throws ServletException
  {
    
    String path
      =getServletConfig()
        .getServletContext()
          .getRealPath(contextRelativePath);
    
    if (path!=null)
    { return new FileResource(new File(path).getAbsoluteFile());
    }
    
    // Fallback case: Use getResource()
    //
    // Sometimes web servers will not return a URL from getResource()
    //   if the resource doesn't exist already
    //
    
    URL url=null;
    try
    {
      url
        =getServletConfig()
          .getServletContext()
            .getResource(contextRelativePath);
    }
    catch (MalformedURLException x)
    { 
      throwServletException
        ("Error getting resource ["+contextRelativePath+"]:"+x,x);
    }
    
    if (url==null)
    { 
      throw new ServletException
        ("ServletContext returned null for getResource() called with "
        +"'"+contextRelativePath+"'"
        );
    }
    
    URI uri=null;
    try
    { uri=url.toURI();
    }
    catch (URISyntaxException x)
    {
      throwServletException
        ("Error getting resource ["+contextRelativePath+"]:"+x,x);
    }
    
    try
    { return Resolver.getInstance().resolve(uri);
    }
    catch (UnresolvableURIException x)
    {
      // Fallback for unknown schemes, etc.
      return new URLResource(url);
    }
  }
  
  public void sendError(ServletResponse servletResponse,Throwable x)
    throws IOException
  {
    HttpServletResponse response=(HttpServletResponse) servletResponse;
    response.setStatus(501);
    
    PrintWriter printWriter=new PrintWriter(response.getWriter());
    printWriter.write(x.toString());

    x.printStackTrace(printWriter);
    printWriter.flush();
  }
  
  
}
