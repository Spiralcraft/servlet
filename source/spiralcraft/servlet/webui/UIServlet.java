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

import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.servlet.kit.HttpServlet;
import spiralcraft.servlet.kit.StandardServletConfig;
import spiralcraft.servlet.vfs.FileServlet;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.SimpleFocus;
//import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.SimpleChannel;


import spiralcraft.vfs.NotStreamableException;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

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
  
  private final HashMap<String,UIService> placeMap
    =new HashMap<String,UIService>();
    
  private HttpServlet staticServlet;
  
  { autoConfigure=true;
  }
  
  /**
   * The servlet that will serve static requests
   * 
   * @param staticServlet
   */
  public void setStaticServlet(HttpServlet staticServlet)
  { this.staticServlet=staticServlet;
  }
  
  @Override
  public void init(ServletConfig config) 
      throws ServletException
  {
    super.init(config);
    if (staticServlet==null)
    { 
      FileServlet fileServlet
        =new FileServlet();
      fileServlet.setDebugLevel(debugLevel);
      staticServlet=fileServlet;
    }
    ServletConfig staticConfig
      =new StandardServletConfig
        (config.getServletName()+".static"
        ,config.getServletContext()
        ,new Properties()
        );
    staticServlet.init(staticConfig);
  }
  
  /**
   * <p>Ensure that a UIServant is bound to the containing path of the requested
   *   path so the binding can reference any Filters or other context
   *   that defines this node of the content tree.
   * </p>
   * 
   * <p>In this method, the containing path is defined as the deepest
   *   container in the context relative path for the request.
   * </p>
   * 
   * @param request
   * @return
   * @throws ServletException
   */
  private UIService ensureContext(HttpServletRequest request)
    throws ServletException
  {
    String contextPath=getContextRelativePath(request);
    contextPath=contextPath.substring(0,contextPath.lastIndexOf("/")+1);
    
    UIService uiServant=placeMap.get(contextPath);
    if (uiServant==null)
    {
      synchronized(placeMap)
      {
        uiServant=placeMap.get(contextPath);
        if (uiServant==null)
        {
          try
          {
            
            Focus<?> focus=FocusFilter.getFocusChain(request);
            if (focus!=null)
            { focus=focus.chain(new SimpleChannel<UIServlet>(this,true));
            }
            else
            { 
              focus
                =new SimpleFocus<UIServlet>
                  (new SimpleChannel<UIServlet>(this,true));
            }
            
            // XXX: We shouldn't blindly create a UIService for every path
            //   that comes in. We need to validate the path at some point to
            //   avoid using resources for bogus paths.
            uiServant=new UIService(contextAdapter,contextPath);
            focus=uiServant.bind(focus);
            placeMap.put(contextPath,uiServant);
            if (debugLevel.isConfig())
            { log.config("Starting UIServlet for path ["+contextPath+"]");
            }
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
        if (debugLevel.isFine())
        { log.fine("Resolved "+request.getServletPath()+" to "+component);
        }
        uiServant.service
          (component
          ,request.getServletPath()
          ,getServletConfig().getServletContext()
          ,request
          ,response
          );
      }
      else
      { 
        if (debugLevel.isFine())
        { log.fine("Delegating "+request.getServletPath());
        }
        staticServlet.service(request,response);
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
      { staticServlet.service(request,response);
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
      { staticServlet.service(request,response);
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
   *   applicable resource mappings.
   * </p>
   * 
   * 
   * @param request
   * @return The RootComponent to handle this request
   */
  private RootComponent resolveComponent(HttpServletRequest request,UIService uiServant)
    throws ServletException,IOException
  {     
    
    String relativePath=getContextRelativePath(request);
    try
    { return uiServant.findComponent(relativePath);
    }
    catch (ContextualException x)
    { throw new ServletException("Error instantiating component for "+relativePath,x);
    }
    
   
  }


  


}
