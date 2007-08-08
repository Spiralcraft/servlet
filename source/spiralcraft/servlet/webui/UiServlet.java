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
import spiralcraft.servlet.HttpFocus;

import spiralcraft.lang.BindException;

import spiralcraft.servlet.webui.components.UiComponent;

import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

import spiralcraft.textgen.ResourceUnit;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.data.persist.XmlAssembly;
import spiralcraft.data.persist.PersistenceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;

import java.io.IOException;

import java.net.URI;

import java.util.HashMap;

/**
 * <P>A Servlet which serves a WebUI Component tree.
 * </P>
 * 
 * <H3>Configuration</H3>
 * 
 * <P>A file, ui.config.xml, contains configuration data for the webui
 *  servlet and is located in the same directory as the requested webui
 *  resource.
 * </P>
 *
 * <H3>Mapping</H3>
 * 
 * <P><B>Path Mapping:</B> When the UiServlet is mapped to a path or is used
 * as the default mapping for an application, and no specific resource is 
 * specified in the request.pathInfo, the "default.webui" resource in the
 * directory specified by the request.servletPath path will be used.
 * </P>
 * 
 * <P><B>Extension Mapping:</B> The UiServlet can be mapped to the *.webui 
 * extension to access to a specific resource directly.
 * </P>
 *
 *
 */
public class UiServlet
  extends HttpServlet
{

  private String defaultResourceName="default.webui";
  private HashMap<String,ResourceUnit> textgenCache
    =new HashMap<String,ResourceUnit>();
  private int resourceCheckFrequencyMs=5000;
  
  private URI defaultSessionTypeURI
    =URI.create("java:/spiralcraft/servlet/webui/Session.assy");
  
  @SuppressWarnings("unchecked") // XXX Need to fix this
  private HttpFocus<?> httpFocus=new HttpFocus();
  
  @Override
  public void init(ServletConfig config)
    throws ServletException
  { 
    super.init(config);
    try
    { httpFocus.init();
    }
    catch (BindException x)
    { throw new ServletException(x.toString(),x);
    }
    
  }
  
  @Override
  protected void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    
    UiComponent component=resolveComponent(request);
    if (component!=null)
    { service(component,request,response);
    }
    else
    { response.sendError(404,"Not Found");
    }
  }
  
  @Override
  protected void doHead(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    UiComponent component=resolveComponent(request);
    if (component!=null)
    { 
      response.setStatus(200);
      response.setContentType(component.getContentType());
      response.flushBuffer();
    }
    else
    { response.sendError(404,"Not Found");
    }
  }
  
  @Override
  protected void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    UiComponent component=resolveComponent(request);
    if (component!=null)
    { service(component,request,response);
    }
    else
    { response.sendError(404,"Not Found");
    }
  } 
  
  private void service
    (UiComponent component
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
    throws IOException
  {
    
    httpFocus.push(this, request, response);
    try
    {
      ServiceContext serviceContext=new ServiceContext();
      serviceContext.setWriter(response.getWriter());
      component.render(serviceContext);
      response.flushBuffer();
    }
    finally
    { httpFocus.pop();
    }
  }
  
  /**
   * Resolve the UI component associated with this request
   * 
   * @param request
   * @return The UIComponent to handle this request
   */
  private UiComponent resolveComponent(HttpServletRequest request)
    throws ServletException,IOException
  { 
    String relativePath=getContextRelativePath(request);
    if (relativePath.endsWith("/"))
    { relativePath=relativePath.concat(defaultResourceName);
    }
   
    ResourceUnit unit=resolveResourceUnit(relativePath);
    if (unit!=null)
    {
      // Only create a session if we have a valid URI
      Session session=getUiSession(request,true);
      try
      { 
        UiComponent component=session.getComponent(relativePath,unit.getUnit());
        return component;
      }
      catch (MarkupException x)
      { 
        throw new ServletException
          ("Error loading webui Component for ["+relativePath+"]:"+x,x);
      }
    }

    return null;
  }

  /**
   * Find or create the ResourceUnit that references the compiled textgen 
   *  doclet.
   * 
   * @param relativePath
   * @return
   * @throws ServletException
   * @throws IOException
   */
  private synchronized ResourceUnit resolveResourceUnit(String relativePath)
    throws ServletException,IOException
  {
    ResourceUnit resourceUnit=textgenCache.get(relativePath);
    
    if (resourceUnit!=null)
    { return resourceUnit;
    }
    
    Resource resource=getResource(relativePath);
    if (!resource.exists())
    { return null;
    }
    
    resourceUnit=new ResourceUnit(resource);
    resourceUnit.setCheckFrequencyMs(resourceCheckFrequencyMs);
    textgenCache.put(relativePath,resourceUnit);
    return resourceUnit;
  }
  
  /**
   * <P>Return the webui Session associated with this request.
   * </P>
   * 
   * <P>The session is scoped to the servletPath of the request, and is
   *   created from the Session.assy.xml assembly class in the servlet
   *   path.
   * </P>
   * 
   * @param request The current HttpServletRequest
   * @param create Whether to create a new Session if none exists
   * @return
   * @throws ServletException
   */
  @SuppressWarnings("unchecked") // Cast result from session.getAttribute()
  private synchronized Session getUiSession
    (HttpServletRequest request
    ,boolean create
    )
    throws ServletException,IOException
  {
    HttpSession session=request.getSession(create);
    if (session==null)
    { return null;
    }
    
    HashMap<String,XmlAssembly<Session>> sessionCache
      =(HashMap<String,XmlAssembly<Session>>) 
        session.getAttribute("spiralcraft.servlet.webui.sessionCache");
    
    if (sessionCache==null)
    { 
      sessionCache=new HashMap<String,XmlAssembly<Session>>();
      session.setAttribute
        ("spiralcraft.servlet.webui.sessionCache",sessionCache);
    }
    
    XmlAssembly<Session> uiSessionXmlAssembly
      =sessionCache.get(request.getServletPath());
    
    if (uiSessionXmlAssembly==null && create)
    { 
      uiSessionXmlAssembly
        =createUiSessionXmlAssembly(request.getServletPath());
      uiSessionXmlAssembly.get().init(httpFocus);
      sessionCache.put(request.getServletPath(), uiSessionXmlAssembly);
    }
    
    if (uiSessionXmlAssembly!=null)
    { return uiSessionXmlAssembly.get();
    }
    else
    { return null;
    }
  }

  private XmlAssembly<Session> createUiSessionXmlAssembly(String resourcePath)
    throws ServletException,IOException
  {
    Resource containerResource=getResource(resourcePath);
    Resource sessionResource=null;
    try
    { 
      sessionResource
        =containerResource.asContainer().getChild("Session.assy.xml");
    }
    catch (UnresolvableURIException x)
    { throw new ServletException(x.toString(),x);
    }
    
    URI typeURI;
    if (sessionResource.exists())
    { typeURI=containerResource.getURI().resolve("Session.assy");
    }
    else
    { typeURI=defaultSessionTypeURI;
    }
    
    try
    { return new XmlAssembly<Session>(typeURI,null);
    }
    catch (PersistenceException x)
    { 
      throw new ServletException
        ("Error loading webui Session from ["+typeURI+"]:"+x,x);
    }
    
  }
}
