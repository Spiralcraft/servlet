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
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Contextual;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.ContextAdapter;
import spiralcraft.servlet.HttpFocus;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.ElementState;
import spiralcraft.textgen.InitializeMessage;
import spiralcraft.textgen.Message;
import spiralcraft.textgen.PrepareMessage;
import spiralcraft.textgen.StateFrame;
import spiralcraft.vfs.Resource;

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
  private static final Message PREPARE_MESSAGE=new PrepareMessage();
  private static final Message COMMAND_MESSAGE=new CommandMessage();
  private static final Message REQUEST_MESSAGE=new RequestMessage();
  
  private final ContextAdapter context;
  
  private UICache uiCache;
  
  private URI defaultSessionTypeURI
    =URI.create("class:/spiralcraft/servlet/webui/Session.assy");
  
  private HttpFocus<?> httpFocus;
  
  public UIService(ContextAdapter context)
  { this.context=context;
  
  }
  
  public Focus<?> bind(Focus<?> focusChain)
    throws BindException
  {
    httpFocus=new HttpFocus<Void>(focusChain);
    focusChain=httpFocus;
    uiCache=new UICache(focusChain);
    return httpFocus;
  }
  
  public RootComponent getRootComponent
    (Resource uiResource
    ,String statePath
    )
    throws ServletException,IOException,MarkupException
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
        ,context.getResource(request.getServletPath())
        ,sessionPath
        ,defaultSessionTypeURI
        ,httpFocus
        ,true
        );

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

        // Set up the ServiceContext with the last frame used.
        serviceContext
        =new ServiceContext
        (response.getWriter(),true,localSession.currentFrame());

        serviceContext.setRequest(request);
        serviceContext.setResponse(response);
        serviceContext.setContextRoot(context.getResource("/"));

        ResourceSession.RequestSyncStatus syncStatus
        =localSession.getRequestSyncStatus(serviceContext.getQuery());

        if (syncStatus==ResourceSession.RequestSyncStatus.OUTOFSYNC)
        { 
          serviceContext.setCurrentFrame(localSession.nextFrame());
          serviceContext.setOutOfSync(true);
          // Clear any pending responsive actions for an out of sync request
          localSession.clearActions();
          if (debugLevel.canLog(Level.DEBUG))
          { 
            log.debug
            ("Out of sync request, ignoring pending responsive actions");
          }
        }
        else if (syncStatus==ResourceSession.RequestSyncStatus.INITIATED)
        { serviceContext.setCurrentFrame(localSession.nextFrame());
        }


        serviceContext.setResourceSession(localSession);

        synchronized (localSession)
        { 
          // Resource state is not multi-threaded
          sequence(component,serviceContext);
        }
      }
      else
      {
        // Non-interactive mode doesn't have to synchronize on the local
        //   resource session and does not maintain session state for the
        //   resource.
        ResourceSession localSession=new ResourceSession();

        serviceContext
        =new ServiceContext(response.getWriter(),true,new StateFrame());

        serviceContext.setRequest(request);
        serviceContext.setResponse(response);
        serviceContext.setContextRoot(context.getResource("/"));

        localSession.setLocalURI
        (request.getRequestURI()
        );
        serviceContext.setResourceSession(localSession);
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
  
  
  /**
   * <p>Service a request for the on a given component with the specified
   *    serviceContext, which has already been associated with a request,
   *    response and a local ResourceSession.
   * </p>
   * 
   * @param component
   * @param serviceContext
   * @throws IOException
   * @throws ServletException
   */
  private void sequence(RootComponent component,ServiceContext serviceContext)
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

    boolean done=false;
    
    //
    // REQUEST
    //
    component.message(serviceContext,REQUEST_MESSAGE,null);
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
    
    
    // XXX consider deferring the frame change to the next request if nothing
    //  changed- ie. don't go into sequence mode
    // XXX it is likely that the frame should be advanced at a low level
    //  wherever data is invalidated to recomp dependant children and not
    //  the whole page
    // XXX for ajax, we also need scoped recompute- a synchronous request
    //  will cancel ajax mode, take a final action, then prepare and render
    
    // This makes everything recompute before rendering
    serviceContext.setCurrentFrame(localSession.nextFrame());
    
    if (!done)
    {

      //
      // PREPARE
      //
      component.message(serviceContext,PREPARE_MESSAGE,null);
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
      component.message(serviceContext,COMMAND_MESSAGE,null);
      done=processRedirect(serviceContext);
    }

    
    if (!done)
    { 
      //
      // RENDER
      //
      render(component,serviceContext);
      done=processRedirect(serviceContext);
      
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
    (RootComponent component
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
    (RootComponent component,ServiceContext context,String actionName)
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
    (RootComponent component
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
    
    serviceContext.getResponse().setStatus(200);
    serviceContext.getResponse().addHeader("Cache-Control","no-cache");
    component.render(serviceContext);
    
    
  }  
}
