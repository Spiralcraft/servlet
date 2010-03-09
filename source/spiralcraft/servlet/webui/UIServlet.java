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
package spiralcraft.servlet.webui;

import spiralcraft.servlet.HttpServlet;
import spiralcraft.servlet.autofilter.spi.FocusFilter;

import spiralcraft.lang.BindException;
//import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.SimpleChannel;


import spiralcraft.vfs.NotStreamableException;

import spiralcraft.text.markup.MarkupException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashMap;

/**
 * <p>A Servlet which serves a WebUI Component tree.
 * </p>
 * 
 * 
 * <h3>Configuration</h3>
 * 
 * <p>A optional file, ui.config.xml, contains configuration data for the webui
 *  servlet and is located in the same directory as the requested webui
 *  resource.
 * </p>
 *
 * <h3>Mapping</h3>
 * 
 * <p><b>Path Mapping:</b> When the UiServlet is mapped to a path or is used
 * as the default mapping for an application, and no specific resource is 
 * specified in the request.pathInfo, the "default.webui" resource in the
 * directory specified by the request.servletPath path will be used.
 * </p>
 * 
 * <p><b>Extension Mapping:</b> The UiServlet can be mapped to the *.webui 
 * extension to access to a specific resource directly.
 * </p>
 *
 *
 */
public class UIServlet
  extends HttpServlet
{

  /**
   * Return whether objects of the specifed type are valid from
   *   request to request.
   *    
   * @param clazz
   * @return
   */
  public static final boolean cachingProhibited(Class<?> clazz)
  {
    return 
      ServiceContext.class.isAssignableFrom(clazz)
      || HttpServletRequest.class.isAssignableFrom(clazz)
      || HttpServletResponse.class.isAssignableFrom(clazz)
      ;
  }
 
//  private Channel<RootComponent> dynamicComponent;
  
  private final HashMap<String,UIService> pathMap
    =new HashMap<String,UIService>();
  
  private String defaultResourceName="default.webui";  
  
  private UIService ensureContext(HttpServletRequest request)
    throws ServletException
  {
    String contextPath=getContextRelativePath(request);
    contextPath=contextPath.substring(0,contextPath.lastIndexOf("/")+1);
    
    UIService uiServant=pathMap.get(contextPath);
    if (uiServant==null)
    {
      synchronized(pathMap)
      {
        uiServant=pathMap.get(contextPath);
        if (uiServant==null)
        {
          try
          {
            
            Focus<?> focus=FocusFilter.getFocusChain(request);

            focus=focus.chain(new SimpleChannel<UIServlet>(this,true));
            uiServant=new UIService(contextAdapter);
            focus=uiServant.bind(focus);
            pathMap.put(contextPath,uiServant);
          }
          catch (BindException x)
          { throw new ServletException(x.toString(),x);
          }
        }
      }
    }
    return uiServant;
  }
    
        
  @Override
  protected void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    UIService uiServant=ensureContext(request);
    

    try
    {
      RootComponent component=resolveComponent(request,uiServant);
      if (component!=null)
      { 
        uiServant.service
          (component
          ,request.getServletPath()
          ,getServletConfig().getServletContext()
          ,request
          ,response
          );
      }
      else
      { response.sendError(404,"Not Found");
      }
    }
    catch (NotStreamableException x)
    {
      if (!request.getRequestURI().endsWith("/"))
      { 
        response.sendRedirect
          (response.encodeRedirectURL(request.getRequestURI()+"/"));
      }
    }
  }
  
  @Override
  protected void doHead(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    UIService uiServant=ensureContext(request);

    try
    {
      RootComponent component=resolveComponent(request,uiServant);
      if (component!=null)
      { 
        response.setStatus(200);
        response.setContentType(component.getContentType());
        response.addHeader("Cache-Control","no-cache");
        response.flushBuffer();
      }
      else
      { response.sendError(404,"Not Found");
      }
    }
    catch (NotStreamableException x)
    {
      if (!request.getRequestURI().endsWith("/"))
      { 
        response.sendRedirect
          (response.encodeRedirectURL(request.getRequestURI()+"/"));
      }
    }
      
  }
  
  @Override
  protected void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    UIService uiServant=ensureContext(request);

    try
    {
      RootComponent component=resolveComponent(request,uiServant);
      if (component!=null)
      { 
        uiServant.service
          (component
          ,request.getServletPath()
          ,getServletConfig().getServletContext()
          ,request
          ,response
          );
      }
      else
      { response.sendError(404,"Not Found");
      }
    }
    catch (NotStreamableException x)
    {
      if (!request.getRequestURI().endsWith("/"))
      { 
        response.sendRedirect
          (response.encodeRedirectURL(request.getRequestURI()+"/"));
      }
    }
  } 
  
  
  /**
   * <p>Resolve the UI component associated with this request, applying any
   *   applicable resource mappings
   * </p>
   * 
   * 
   * @param request
   * @return The RootComponent to handle this request
   */
  private RootComponent resolveComponent(HttpServletRequest request,UIService uiServant)
    throws ServletException,IOException
  { 
    // Run the dynamic component instead of the default behavior for this
    //   request
//    if (dynamicComponent!=null)
//    {
//      RootComponent component=dynamicComponent.get();
//      if (component!=null)
//      { return component;
//      }
//    }
    
    
    String relativePath=getContextRelativePath(request);
    if (relativePath.endsWith("/"))
    { relativePath=relativePath.concat(defaultResourceName);
    }
   
    
    try
    { 
      RootComponent component
        =uiServant.getRootComponent
          (getResource(relativePath)
          ,relativePath
          );
      
      if (component==null)
      {
        // Look in the fallback dir?
      }
      return component;
    }
    catch (MarkupException x)
    { 
      throw new ServletException
        ("Error loading webui Component for ["+relativePath+"]:"+x,x);
    }
    
    
  }


  


}
