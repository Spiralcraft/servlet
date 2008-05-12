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
import spiralcraft.servlet.autofilter.FocusFilter;

import spiralcraft.lang.BindException;


import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;
import spiralcraft.vfs.NotStreamableException;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.ElementState;
import spiralcraft.textgen.InitializeMessage;
import spiralcraft.textgen.PrepareMessage;

import spiralcraft.data.persist.XmlAssembly;
import spiralcraft.data.persist.PersistenceException;

import spiralcraft.net.http.VariableMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;

import java.io.IOException;

import java.net.URI;

import java.util.HashMap;

import java.util.LinkedList;

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
public class UIServlet
  extends HttpServlet
{

  private String defaultResourceName="default.webui";
  private UICache uiCache;
  
  private URI defaultSessionTypeURI
    =URI.create("java:/spiralcraft/servlet/webui/Session.assy");
  
  @SuppressWarnings("unchecked") // XXX Need to fix this
  private HttpFocus<?> httpFocus;
  
  @Override
  public void init(ServletConfig config)
    throws ServletException
  { 
    super.init(config);
    
  }
  
  private void checkInit(HttpServletRequest request)
    throws ServletException
  {
    if (httpFocus==null)
    {
      // Initialize the local HTTP Focus with its parent that's always passed
      //   via the request
      
      try
      {
        HttpFocus<?> focus=new HttpFocus<Void>();
        focus.init();
        focus.setParentFocus(FocusFilter.getFocusChain(request));
        httpFocus=focus;
      }
      catch (BindException x)
      { throw new ServletException(x.toString(),x);
      }
      uiCache=new UICache(this,httpFocus);
    }
  }
  
  @Override
  protected void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    checkInit(request);
    

    try
    {
      UIComponent component=resolveComponent(request);
      if (component!=null)
      { service(component,request,response);
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
    checkInit(request);

    try
    {
      UIComponent component=resolveComponent(request);
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
    checkInit(request);

    try
    {
      UIComponent component=resolveComponent(request);
      if (component!=null)
      { service(component,request,response);
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
  
  private void service
    (UIComponent component
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
    throws IOException,ServletException
  {
    
    httpFocus.push(this, request, response);

    // Move to UIServlet from parameter in this page 
    response.setBufferSize(16384);
    try
    {
      ServiceContext serviceContext
        =new ServiceContext(response.getWriter(),true);
      
      serviceContext.setRequest(request);
      serviceContext.setResponse(response);
      
      Session session=getUiSession(request,true);
      ResourceSession localSession=session.getResourceSession(component);
      
      
      if (localSession==null)
      { 
        localSession=new ResourceSession();
        localSession.setLocalURI
          (request.getRequestURI()
          );
        session.setResourceSession(component,localSession);
      }
      
      serviceContext.setResourceSession(localSession);
      
      ElementState oldState=localSession.getRootState();
      
      if (oldState==null)
      { 
        // Initialize a fresh state
        serviceContext.setState(component.createState());
        // Set up state structure and register "initial" events
        component.message(serviceContext,new InitializeMessage(),null);
      }
      else
      { 
        // Restore state
        serviceContext.setState(oldState);
      }
      
      if (request.getContentLength()>0)
      {
        
      }

      
      handleAction(component,serviceContext);

      localSession.clearActions();
      
      if (serviceContext.getRedirectURI()!=null)
      {
        // Redirect after action
        response.sendRedirect
          (response.encodeRedirectURL
            (serviceContext.getRedirectURI().toString())
          );
      }
      else
      {

        component.message(serviceContext,new PrepareMessage(),null);
      
        if (serviceContext.getRedirectURI()!=null)
        {
          // Redirect after prepare
          response.sendRedirect
            (response.encodeRedirectURL
              (serviceContext.getRedirectURI().toString())
            );
        }
        else
        { render(component,serviceContext);
        }
      }
      
      
      ElementState newState=serviceContext.getState();
      if (newState!=oldState)
      { 
        // Cache the state for the next iteratio
        localSession.setRootState(newState);
      }
      
      response.getWriter().flush();
      response.flushBuffer();
      
    }
    finally
    { httpFocus.pop();
    }
  }
  
  private void handleAction
    (UIComponent component
    ,ServiceContext context
    )
  {
    // long time=System.nanoTime();

    VariableMap vars=context.getQuery();
    if (vars!=null)
    {

      String actionName=vars.getOne("action");
      if (actionName!=null)
      {
        Action action=context.getResourceSession().getAction(actionName);
        if (action!=null)
        {
          LinkedList<Integer> path=new LinkedList<Integer>();
        
          for (int i:action.getTargetPath())
          { path.add(i);
          }
          component.message
            (context
            ,new ActionMessage(action)
            ,path
            );
        }
      }
    }


    // System.err.println("UIServler.handleAction: "+(System.nanoTime()-time));
  }
  
  private void render
    (UIComponent component
    ,ServiceContext serviceContext
    )
    throws IOException,ServletException
  {
    //long time=System.nanoTime();
    
    serviceContext.getResponse().setContentType(component.getContentType());
    serviceContext.getResponse().setStatus(200);
    serviceContext.getResponse().addHeader("Cache-Control","no-cache");
    component.render(serviceContext);
    
    // System.err.println("UIServler.render: "+(System.nanoTime()-time));
    
  }
  
  /**
   * <P>Resolve the UI component associated with this request, applying any
   *   applicable resource mappings
   * </P>
   * 
   * 
   * @param request
   * @return The UIComponent to handle this request
   */
  private UIComponent resolveComponent(HttpServletRequest request)
    throws ServletException,IOException
  { 
    String relativePath=getContextRelativePath(request);
    if (relativePath.endsWith("/"))
    { relativePath=relativePath.concat(defaultResourceName);
    }
   
    String resourcePath=relativePath;
    
    try
    { 
      UIComponent component=uiCache.getUI(resourcePath);
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


  
  /**
   * <p>Return the webui Session associated with this request.
   * </p>
   * 
   * <p>The session is scoped to the servletPath of the request, and is
   *   created from the Session.assy.xml assembly class in the servlet
   *   path.
   * </p>
   * 
   * <p>It is intended for the servletPath to indicate the request directory
   *   and the pathInfo to resolve the specific UI resource.
   * </p>
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
    if (containerResource.asContainer()==null)
    { containerResource=containerResource.getParent();
    }
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
