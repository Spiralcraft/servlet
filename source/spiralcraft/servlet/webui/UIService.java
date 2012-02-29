//
//Copyright (c) 2010 Michael Toth
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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.kit.ContextAdapter;
import spiralcraft.servlet.kit.HttpFocus;
import spiralcraft.servlet.webui.kit.PortSession;
import spiralcraft.textgen.PrepareMessage;
import spiralcraft.textgen.RenderMessage;
import spiralcraft.ui.NavContext;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;


import spiralcraft.app.Message;
import spiralcraft.app.State;
import spiralcraft.app.InitializeMessage;
import spiralcraft.app.StateFrame;

import spiralcraft.common.ContextualException;

/**
 * <p>Manages a set of WebUI interfaces and their session state 
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
 */
public class UIService
  implements Contextual
{

  private static final ClassLog log
    =ClassLog.getInstance(UIService.class);
  private static final Level debugLevel
    =ClassLog.getInitialDebugLevel(UIService.class,null);
  
  private static final Message INITIALIZE_MESSAGE=new InitializeMessage();
  
  private final ContextAdapter context;
  
  private UICache uiCache;
  private String contextRelativePath;
  private final HashMap<String,Resource> uiResourceMap
    =new HashMap<String,Resource>();
  private final HashSet<String> bypassSet
    =new HashSet<String>();
  
  private URI defaultSessionTypeURI
    =URI.create("class:/spiralcraft/servlet/webui/Session");
  
  private HttpFocus<?> httpFocus;
  @SuppressWarnings("rawtypes")
  private Focus<NavContext> navContextFocus;
  
  public UIService(ContextAdapter context,String contextRelativePath)
  { 
    this.context=context;
    this.contextRelativePath=contextRelativePath;
  }
  
  @Override
  public Focus<?> bind(Focus<?> focusChain)
    throws BindException
  {
    httpFocus=new HttpFocus<Void>(focusChain);
    focusChain=httpFocus;
    uiCache=new UICache(focusChain);
    navContextFocus
      =LangUtil.findFocus(NavContext.class,focusChain);
    return httpFocus;
  }
  
  public NavContext<?,?> getNavContext()
  { 
    if (navContextFocus!=null)
    { return navContextFocus.getSubject().get();
    }
    else
    { return null;
    }
  }
  
  public RootComponent findComponent(String relativePath)
    throws ContextualException,IOException,ServletException
  {

    if (bypassSet.contains(relativePath))
    { return null;
    }
    
    Resource resource=uiResourceMap.get(relativePath);
    if (resource==null)
    {
      synchronized (uiResourceMap)
      { 
        synchronized (bypassSet)
        {
          if (bypassSet.contains(relativePath))
          { return null;
          }
        }
        
        resource=uiResourceMap.get(relativePath);
        if (resource==null)
        { resource=findUIResource(relativePath);
        }
        if (resource!=null)
        { uiResourceMap.put(relativePath,resource);
        }
        else
        { bypassSet.add(relativePath);
        }
      }
    }
    
    if (resource==null)
    { return null;
    }
    
    return getRootComponent(resource,relativePath);
  }
  
  protected Resource findUIResource(String relativePath)
    throws IOException, ServletException
  {
    
    Resource resource=null;
    String relativeResourcePath;
    
    
    if (!relativePath.endsWith(".webui"))
    { 
      if (relativePath.endsWith("/"))
      { relativeResourcePath=relativePath+"default.webui";
      }
      else
      { 
        if (context.getResource(relativePath).exists())
        { 
          // Static resource in standard context takes precedence
          return null;
        }
        relativeResourcePath=relativePath+".webui";
      }
    }
    else
    { relativeResourcePath=relativePath;
    }
    
    if (resource==null) 
    { 
      resource=context.getResource(relativeResourcePath);      
      if (!resource.exists())
      { resource=null;
      }
    }
    
    if (resource==null)
    { 
      resource=Resolver.getInstance().resolve
        ("context://code"+relativeResourcePath);

      if (!resource.exists())
      { resource=null;
      }
    }
    

    if (resource==null)
    {
      // Fall back to data-driven UI
      NavContext<?,?> navContext=getNavContext();
    
      if (navContext!=null)
      { 
        URI viewResourceURI=navContext.getViewResourceURI();
        if (viewResourceURI!=null)
        {
          if (viewResourceURI.isAbsolute())
          { resource=Resolver.getInstance().resolve(viewResourceURI);
          }
          else
          { resource=context.getResource(viewResourceURI.getPath());
          }
          if (!resource.exists())
          { resource=null;
          }
        }
        
      }
    }
        
    return resource;
  }
  
  
  public RootComponent getRootComponent
    (Resource uiResource
    ,String statePath
    )
    throws ServletException,IOException,ContextualException
  { 
    return uiCache.getUI
      (uiResource
      ,statePath
      );
  }
  
  /**
   * Service a request
   * 
   * @param component The WebUI component
   * 
   * @param sessionPath The "sub-session" of the main servlet session,
   *   often derived from Request.servletPath and Request.pathInfo
   *   
   * @param servletContext The servlet context
   * 
   * @param request The request
   * @param response The response
   * @throws IOException
   * @throws ServletException
   */
  public void service
    (RootComponent component
    ,String sessionPath
    ,ServletContext servletContext
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
  throws IOException,ServletException
  {

    httpFocus.push(servletContext, request, response);

    // Move to UIServlet from parameter in this page 
    response.setBufferSize(16384);

    ServiceContext serviceContext=null;

    try
    {
      Session session
        =Session.get
        (request
        ,context.getResource(contextRelativePath)
        ,contextRelativePath
        ,defaultSessionTypeURI
        ,httpFocus
        ,true
        );

      boolean interactive=true;
      if (interactive)
      {
        // Interactive mode maintains a session scoped state for a resource
        //   which must be synchronized.

        PortSession localSession=session.getResourceSession(component);

        if (localSession==null)
        { 

          synchronized (session)
          {
            localSession=session.getResourceSession(component);
            if (localSession==null)
            {
              localSession=new PortSession();
              localSession.setLocalURI
              (request.getRequestURI()
              );
              session.setResourceSession(component,localSession);
            }
          }
        }

        // Set up the ServiceContext with the last frame used.
        serviceContext
          =new ServiceContext
          (response.getWriter(),true,localSession.currentFrame());

        serviceContext.setRequest(request);
        serviceContext.setResponse(response);
        serviceContext.setServletContext(context);
        serviceContext.setPortSession(localSession);
        
        serviceRootPort(serviceContext,component,request,response);
      }
      else
      {
        // Non-interactive mode doesn't have to synchronize on the local
        //   resource session and does not maintain session state for the
        //   resource.
        PortSession localSession=new PortSession();

        serviceContext
        =new ServiceContext(response.getWriter(),true,new StateFrame());

        serviceContext.setRequest(request);
        serviceContext.setResponse(response);

        localSession.setLocalURI
        (request.getRequestURI()
        );
        serviceContext.setPortSession(localSession);
        sequence(component,serviceContext);

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
  
  private void serviceRootPort
    (ServiceContext serviceContext
    ,Component component
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
    throws IOException,ServletException
  {
    PortSession localSession=serviceContext.getPortSession();
    
    VariableMap query=serviceContext.getQuery();
    String requestedState=query!=null?query.getFirst("lrs"):null;
    PortSession.RequestSyncStatus syncStatus
      =localSession.getRequestSyncStatus(requestedState);

    String outOfBand
      =query!=null
      ?query.getFirst("oob")
      :null;
      
    if (outOfBand!=null && !outOfBand.isEmpty())
    {
      // Process out-of-band request
      outOfBand=outOfBand.intern(); 
      
      if (outOfBand=="sessionSync")
      { 
        switch (syncStatus)
        {
          case INITIATED:
          case OUTOFSYNC:
            response.getWriter().write("0");
            break;
          default:
            response.getWriter().write
              (Integer.toString
                (request.getSession().getMaxInactiveInterval()*1000)
              );
            break;

        }
        response.setContentType("text/plain");
        response.setHeader("Cache-Control","no-cache");
        response.setIntHeader("Max-Age",0);
        response.setDateHeader("Expires",0);
        
        response.setStatus(200);

      }
      response.getWriter().flush();
      response.flushBuffer();
    }
    else
    {
      // Process interactive request
      
      if (syncStatus==PortSession.RequestSyncStatus.OUTOFSYNC)
      { 
        serviceContext.setCurrentFrame(localSession.nextFrame());
        serviceContext.setOutOfSync(true);
        // Clear any pending responsive actions for an out of sync request
        localSession.clearActions();
        if (debugLevel.isDebug())
        { 
          log.debug
          ("Out of sync request, ignoring pending responsive actions");
        }
      }
      else if (syncStatus==PortSession.RequestSyncStatus.INITIATED)
      { 
        serviceContext.setInitial(true);
        serviceContext.setCurrentFrame(localSession.nextFrame());
        if (debugLevel.isDebug())
        { log.debug("Initializing session for "+localSession.getLocalURI());
        }
      }



      synchronized (localSession)
      { 
        // Resource state is not multi-threaded
        sequence(component,serviceContext);
      }
    }
        
        // End Port Specific Processing
  }
  
  /**
   * <p>Service a request for the  given component with the specified
   *    serviceContext, which has already been associated with a request,
   *    response and a local ResourceSession.
   * </p>
   * 
   * @param component
   * @param serviceContext
   * @throws IOException
   * @throws ServletException
   */
  private void sequence(Component component,ServiceContext serviceContext)
    throws IOException,ServletException
  {
//    HttpServletRequest request=serviceContext.getRequest();
    HttpServletResponse response=serviceContext.getResponse();
    PortSession localSession=serviceContext.getPortSession();

    State oldState=localSession.getState();
    if (oldState==null)
    { 
      if (debugLevel.isDebug())
      { log.debug("Initializing state tree for "+localSession.getLocalURI());
      }
      
      serviceContext.setState(component.createState());
      
      // Set up state structure and register "initial" events
      serviceContext.dispatch
        (INITIALIZE_MESSAGE,component,null);
    }
    else
    { 
      // Restore state
      serviceContext.setState(oldState);
    }

    boolean done=false;
    
    //
    // REQUEST
    //
    if (debugLevel.isFine())
    { 
      log.fine("Dispatching REQUEST message for frame "
        +serviceContext.getFrame());
    }
    serviceContext.dispatch(RequestMessage.INSTANCE,component,null);
    done=processRedirect(serviceContext);
    
    if (!done)
    {
     
      //
      // ACTION
      //
      handleAction(component,serviceContext);

      localSession.clearActions();
      done=processRedirect(serviceContext);
    }
    
    
    if (!done)
    { 
      // SYNC RESPONSE
      generateResponse(serviceContext,localSession,component);
    }
    
    
    State newState=serviceContext.getState();
    if (newState!=oldState)
    { 
      // Cache the state for the next iteration
      localSession.setState(newState);
    }
      
    response.getWriter().flush();
    response.flushBuffer();
    
  }
  
  /**
   * <p>Generate a fresh page for the client 
   * </p>
   * 
   * @param serviceContext
   * @param localSession
   * @param component
   * @throws ServletException
   * @throws IOException
   */
  private void generateResponse
    (ServiceContext serviceContext
    ,PortSession localSession
    ,Component component
    ) throws ServletException, IOException
  {
    
    
    // XXX consider deferring the frame change to the next request if nothing
    //  changed- ie. don't go into sequence mode
    // XXX it is likely that the frame should be advanced at a low level
    //  wherever data is invalidated to recomp dependant children and not
    //  the whole page
    // XXX for ajax, we also need scoped recompute- a synchronous request
    //  will cancel ajax mode, take a final action, then prepare and render
    
    // This makes everything recompute before rendering
    serviceContext.setCurrentFrame(localSession.nextFrame());
    boolean done=false;
    
    if (!done)
    {

      //
      // PREPARE
      //
      if (debugLevel.isFine())
      { 
        log.fine("Dispatching PREPARE message for frame "
          +serviceContext.getFrame());
      }
      serviceContext.dispatch(PrepareMessage.INSTANCE,component,null);
      done=processRedirect(serviceContext);
    }
    
    if (!done)
    {
      // XXX: A command may change the internal state, but components
      //  will not pick it up unless the command triggers a state change, but
      //  if a command triggers a state change at this point, not all
      //  components will pick up that change before render.
      // 
      // Therefore all page modifying state needs to take place before the
      //   completion of "prepare". 
       
      //
      // COMMAND
      //
      if (debugLevel.isFine())
      {
        log.fine("Dispatching COMMAND message for frame "
            +serviceContext.getFrame());
      }
      serviceContext.dispatch(CommandMessage.INSTANCE,component,null);
      done=processRedirect(serviceContext);
    }

    
    if (!done)
    { 
      //
      // RENDER
      //
      if (debugLevel.isFine())
      {
        log.fine("Dispatching RENDER message for frame "
          +serviceContext.getFrame());
      }
      render(component,serviceContext);
      done=processRedirect(serviceContext);
      
    }
      

  }
  
  /**
   * <p>Check to see whether a redirect needs to be performed. If so,
   *   perform the redirect and return true, otherwise return false
   * </p>
   * @param serviceContext
   * @return
   * @throws ServletException
   * @throws IOException
   */
  private boolean processRedirect(ServiceContext serviceContext)
    throws ServletException,IOException
  {
    if (serviceContext.getRedirectURI()!=null)
    {
      HttpServletResponse response=serviceContext.getResponse();

      response.sendRedirect
        (response.encodeRedirectURL
          (serviceContext.getRedirectURI().toString())
        );
      return true;
    }
    return false;
  }
  
  /**
   * <p>Handle any actions invoked by this request.
   * </p>
   * 
   * @param component
   * @param context
   */
  private void handleAction
    (Component component
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
    (Component component,ServiceContext context,String actionName)
  {
    List<Action> actions
      =context.getPortSession().getActions(actionName);
    if (actions!=null && !actions.isEmpty())
    {
      for (Action action:actions)
      {
        context.dispatch
          (new ActionMessage(action)
          ,component
          ,action.getTargetPath()
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
    (Component component
    ,ServiceContext serviceContext
    )
    throws IOException,ServletException
  {
    if (serviceContext.getContentType()!=null)
    { 
      serviceContext.getResponse().setContentType
        (serviceContext.getContentType());
    }
    else
    { serviceContext.getResponse().setContentType(component.getContentType());
    }
    
    Integer code
      =(Integer) serviceContext.getRequest().getAttribute
        ("javax.servlet.error.status_code");
    if (code!=null)
    { serviceContext.getResponse().setStatus(code);
    }
    else
    { serviceContext.getResponse().setStatus(200);
    }
    serviceContext.getResponse().addHeader("Cache-Control","no-cache");
    serviceContext.dispatch(RenderMessage.INSTANCE,component,null);
    
    
  }  
}
