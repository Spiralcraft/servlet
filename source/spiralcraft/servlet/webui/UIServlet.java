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
import spiralcraft.log.ClassLog;


import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;
import spiralcraft.vfs.NotStreamableException;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.ElementState;
import spiralcraft.textgen.InitializeMessage;
import spiralcraft.textgen.Message;
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
import java.util.List;

import java.util.LinkedList;

/**
 * <p>A Servlet which serves a WebUI Component tree.
 * </p>
 * 
 * <h3>Request processing stages
 * </h3>
 * 
 * <ul>
 *   <li><h4>Resource Session Initialization (at beginning of new session)</h4>
 *     <p>Provides all components in the tree with an opportunity to set up their
 *        initial session-state and register any permanent actions.
 *     </p>
 *   </li>
 *   
 *   <li><h4>Action processing</h4>
 *     <p>Registers user/client actions encoded in the incoming HTTP request,
 *       some of which may be in response to the UI state rendered in the
 *       previous cycle. Actions in response to a previously rendered
 *       UI state will complete any triggered activities. "Incoming" actions
 *       not associated with the previous state will enqueue any triggered
 *       commands for completion during the Command stage.
 *     </p>
 *   </li>
 *   
 *   <li><h4>Preparation of new state</h4>
 *     <p>Provides all components in the tree with an opportunity to reset
 *       their state for command processing and UI rendering.
 *     </p>
 *   </li>
 *   
 *   <li><h4>Command processing</h4>
 *     <p>Executes any queued commands to perform computations before rendering.
 *     </p>
 *   </li>
 *   
 *   <li><h4>Rendering</h4>
 *     <p>Components generate UI elements and register UI callback actions.
 *     </p>
 *   </li>
 * </ul>
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
  private static final ClassLog log=ClassLog.getInstance(UIServlet.class);
  
  private static final Message INITIALIZE_MESSAGE=new InitializeMessage();
  private static final Message PREPARE_MESSAGE=new PrepareMessage();
  private static final Message COMMAND_MESSAGE=new CommandMessage();
  private static final Message REQUEST_MESSAGE=new RequestMessage();

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
  
  private String defaultResourceName="default.webui";
  private UICache uiCache;
  
  private URI defaultSessionTypeURI
    =URI.create("class:/spiralcraft/servlet/webui/Session.assy");
  
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
    
    ServiceContext serviceContext=null;
    
    try
    {
      
      serviceContext=new ServiceContext(response.getWriter(),true);
      
      serviceContext.setRequest(request);
      serviceContext.setResponse(response);
      serviceContext.setServlet(this);
      
      Session session=getUiSession(request,true);

      boolean interactive=true;
      if (interactive)
      {
        // Interactive mode maintains a session scoped state for a resource
        //   which must be synchronized.
        
        ResourceSession localSession=session.getResourceSession(component);
      
        if (localSession==null)
        { 
        
          synchronized (session)
          {
            localSession=session.getResourceSession(component);
            if (localSession==null)
            {
              localSession=new ResourceSession();
              localSession.setLocalURI
                (request.getRequestURI()
                );
              session.setResourceSession(component,localSession);
            }
          }
        }
      

        serviceContext.setResourceSession(localSession);
      
        synchronized (localSession)
        { 
          // Resource state is not multi-threaded
          service(component,serviceContext);
        }
      }
      else
      {
        // Non-interactive mode doesn't have to synchronize on the local
        //   resource session and does not maintain session state for the
        //   resource.
        
        ResourceSession localSession=new ResourceSession();
        localSession.setLocalURI
          (request.getRequestURI()
          );
        serviceContext.setResourceSession(localSession);
        service(component,serviceContext);
        
      }
      
      
    }
    finally
    { 
      httpFocus.pop();
      if (serviceContext!=null)
      {
        serviceContext.release();
        serviceContext=null;
      }
    }
  }
  
  /**
   * <p>Service a request on a given component with the specified
   *    serviceContext, which has already been associated with a request,
   *    response and a local ResourceSession.
   * </p>
   * 
   * @param component
   * @param serviceContext
   * @throws IOException
   * @throws ServletException
   */
  private void service(UIComponent component,ServiceContext serviceContext)
    throws IOException,ServletException
  {
    HttpServletRequest request=serviceContext.getRequest();
    HttpServletResponse response=serviceContext.getResponse();
    ResourceSession localSession=serviceContext.getResourceSession();

    ElementState oldState=localSession.getRootState();
    if (oldState==null)
    { 
      // Initialize a fresh state
      serviceContext.setState(component.createState());
      // Set up state structure and register "initial" events
      component.message(serviceContext,INITIALIZE_MESSAGE,null);
    }
    else
    { 
      // Restore state
      serviceContext.setState(oldState);
    }
      
    if (request.getContentLength()>0)
    {
    
    }

    component.message(serviceContext,REQUEST_MESSAGE,null);
      
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

      //
      // PREPARE
      //
      component.message(serviceContext,PREPARE_MESSAGE,null);
      if (serviceContext.getRedirectURI()!=null)
      {
        // Redirect after prepare
        response.sendRedirect
          (response.encodeRedirectURL
            (serviceContext.getRedirectURI().toString())
          );
      }
      else
      {
       
        //
        // COMMAND
        //
        component.message(serviceContext,COMMAND_MESSAGE,null);
      
        if (serviceContext.getRedirectURI()!=null)
        {
          // Redirect after commands
          response.sendRedirect
            (response.encodeRedirectURL
              (serviceContext.getRedirectURI().toString())
            );
        }
        else
        { 
          //
          // RENDER
          //
          render(component,serviceContext);
        }
       
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
  
  /**
   * <p>Handle any actions invoked by this request.
   * </p>
   * 
   * @param component
   * @param context
   */
  private void handleAction
    (UIComponent component
    ,ServiceContext context
    )
  {
    // long time=System.nanoTime();

    // Fire any actions referenced in the URI "action" parameter
    VariableMap vars=context.getQuery();
    if (vars!=null)
    {

      List<String> actionNames=vars.get("action");

//      log.fine("action="+actionNames);
      if (actionNames!=null)
      {
        for (String actionName:actionNames)
        { 
          fireAction(component,context,actionName);
        }
      }
    }
    
    // Dequeue and fire any actions that have been subsequently queued
    List<String> actionNames=context.dequeueActions();
    while (actionNames!=null)
    {
      for (String actionName:actionNames)
      { fireAction(component,context,actionName);
      }
      actionNames=context.dequeueActions();
    }


    // System.err.println("UIServler.handleAction: "+(System.nanoTime()-time));
  }
  
  /**
   * <p>Fire an individual action by name.
   * </p>
   * 
   * <p>Firing an action may indirectly result in other actions being
   *   queued.
   * </p>
   * 
   * @param component
   * @param context
   * @param actionName
   */
  private void fireAction
    (UIComponent component,ServiceContext context,String actionName)
  {
    List<Action> actions
      =context.getResourceSession().getActions(actionName);
    if (actions!=null && !actions.isEmpty())
    {
      for (Action action:actions)
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
    else
    { log.warning("Unknown action "+actionName);
    }
    
  }
      
  /**
   * <p>Send all relevant response headers and call
   *   component.render(serviceContext)
   * </p>
   * 
   * @param component
   * @param serviceContext
   * @throws IOException
   * @throws ServletException
   */
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
