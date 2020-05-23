//
//Copyright (c) 1998,2012 Michael Toth
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
package spiralcraft.servlet.webui.kit;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.app.InitializeMessage;
import spiralcraft.app.Message;
import spiralcraft.app.StateFrame;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.profiler.ProfilerAgent;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ActionMessage;
import spiralcraft.servlet.webui.CommandMessage;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.RequestMessage;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.ServiceRootComponent;
import spiralcraft.servlet.webui.ServiceRootComponentState;
import spiralcraft.textgen.PrepareMessage;
import spiralcraft.textgen.RenderMessage;
import spiralcraft.util.Sequence;

/**
 * <p>Sequences the set of events that enable a component to receive, process
 *   and respond to input
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

 * @author mike
 *
 */
public class UISequencer
{

  private static final StateFrame INIT_FRAME=new StateFrame();
  private static final ClassLog log
    =ClassLog.getInstance(UISequencer.class);
  private static final Level logLevel
    =ClassLog.getInitialDebugLevel(UISequencer.class,Level.INFO);
 
  
  public void service
    (ServiceContext serviceContext
    ,ServiceRootComponent component
    ,PortSession localSession
    ,boolean external
    )
  {  
    VariableMap query=serviceContext.getQuery();
    
    String requestedState=query!=null?query.getFirst("lrs"):null;
    
    PortSession.RequestSyncStatus syncStatus
      =localSession.getRequestSyncStatus(requestedState);
    
    PortSession containingPortSession
      =serviceContext.getPortSession();
    serviceContext.setPortSession(localSession);
    
    HttpServletResponse response=serviceContext.getResponse();
    HttpServletRequest request=serviceContext.getRequest();
    
    try
    {
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
              // Initiated means no LRS sent in the request
              log.warning("OOB request must have an LRS");
              response.getWriter().write("0");
              break;
            case OUTOFSYNC:
            case NOSTATE:
              // OutOfSync should have already been intercepted
              // Initiated means no LRS sent in the request
              log.warning("OOB request is not in sync");
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
      else if (external)
      {
        // Process interactive request
        
        if (syncStatus==PortSession.RequestSyncStatus.NOSTATE)
        {
          
          if (logLevel.isDebug())
          { log.debug("Initializing state tree for "+localSession.getLocalURI());
          }
      
          ServiceRootComponentState state=component.createState();
          serviceContext.setState(state);
          serviceContext.setCurrentFrame(INIT_FRAME);
          localSession.setState(state);
          
          dispatchProfiled("INITIALIZE",serviceContext,InitializeMessage.INSTANCE,component,null);

          serviceContext.setInitial(true);
          
          serviceContext.setCurrentFrame(localSession.nextFrame());
          if (logLevel.isDebug())
          { log.debug("Initializing session for "+localSession.getLocalURI());
          }
        }
        else if (syncStatus==PortSession.RequestSyncStatus.OUTOFSYNC)
        { 
          serviceContext.setCurrentFrame(localSession.nextFrame());
          serviceContext.setOutOfSync(true);
          // Clear any pending responsive actions for an out of sync request
          localSession.clearActions();
          if (logLevel.isDebug())
          { 
            log.debug
            ("Out of sync request, ignoring pending responsive actions");
          }
        }
        else if (syncStatus==PortSession.RequestSyncStatus.INITIATED)
        { 
          serviceContext.setInitial(true);
          serviceContext.setCurrentFrame(localSession.nextFrame());
          if (logLevel.isDebug())
          { log.debug("Initializing session for "+localSession.getLocalURI());
          }
        }
        
        sequence(serviceContext,component,localSession);
        
        response.getWriter().flush();
        try
        { response.flushBuffer();   
        }
        catch (IOException x)
        { log.warning("Caught IOException finishing response: "+x.getMessage());
        }
      }
      else
      {
        if (syncStatus==PortSession.RequestSyncStatus.INITIATED)
        { 
          // TODO: An initiating request should return a response code
          //          that forces a page reload on the client
          log.warning("Async request is initiating conversation");
          response.sendError(409);
        }        
        else
        {
          
          // Ignore out-of-sync status when addressing internal port-
          //   the frame in the port is likely to be ahead of the frame
          //   in the page which is tied to the LRS.
          sequence(serviceContext,component,localSession);
        }
        
        response.getWriter().flush();
        try
        { response.flushBuffer();   
        }
        catch (IOException x)
        { log.warning("Caught IOException finishing response: "+x.getMessage());
        }
        
      }
    }
    catch (IOException x)
    { throw new RuntimeException(x);
    }
    finally
    { serviceContext.setPortSession(containingPortSession);
    }

  }
  
  private void sequence
    (ServiceContext serviceContext
    ,ServiceRootComponent component
    ,PortSession localSession
    )
  {
    
    ServiceRootComponentState oldState=localSession.getState();
    // Restore state
    serviceContext.setState(oldState);

    boolean done=false;

    //
    // REQUEST
    //
    if (logLevel.isTrace())
    { 
      log.fine("Dispatching REQUEST message for frame "
        +serviceContext.getFrame());
    }
    dispatchProfiled
      ("REQUEST",serviceContext,RequestMessage.INSTANCE,component,null);
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

      // XXX consider deferring the frame change to the next request if nothing
      //  changed- ie. don't go into sequence mode
      // XXX it is likely that the frame should be advanced at a low level
      //  wherever data is invalidated to recomp dependant children and not
      //  the whole page
      // XXX for ajax, we also need scoped recompute- a synchronous request
      //  will cancel ajax mode, take a final action, then prepare and render
    
      // This makes everything recompute before rendering
      if (!serviceContext.getInitial() 
          || ((ServiceRootComponentState) serviceContext.getState()).isDirty()
          )
      { serviceContext.setCurrentFrame(localSession.nextFrame());
      }
      else
      { 
        // Don't advance the frame if we already did so in the initial request
        //   and nothing has changed.
      }
      
      // SYNC RESPONSE
      generateResponse(serviceContext,localSession,component);
    }    
    
    ServiceRootComponentState newState
      =(ServiceRootComponentState) serviceContext.getState();
    if (newState!=oldState)
    { 
      // Cache the state for the next iteration
      localSession.setState(newState);
    }
      
 
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
    ) 
  {
    
    
    boolean done=false;
    
    if (!done)
    {

      //
      // PREPARE
      //
      if (logLevel.isFine())
      { 
        log.fine("Dispatching PREPARE message for frame "
          +serviceContext.getFrame());
      }
      // Set up state structure and register "initial" events
      dispatchProfiled
        ("PREPARE",serviceContext,PrepareMessage.INSTANCE,component,null);
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
      if (logLevel.isFine())
      {
        log.fine("Dispatching COMMAND message for frame "
            +serviceContext.getFrame());
      }
      dispatchProfiled("COMMAND",serviceContext,CommandMessage.INSTANCE,component,null);
      done=processRedirect(serviceContext);
    }

    
    if (!done)
    { 
      //
      // RENDER
      //
      if (logLevel.isFine())
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
  {
    if (serviceContext.getRedirectURI()!=null)
    {
      HttpServletResponse response=serviceContext.getResponse();

      try
      {
        response.sendRedirect
          (response.encodeRedirectURL
            (serviceContext.getRedirectURI().toString())
          );
      }
      catch (IOException x)
      { throw new RuntimeException(x);
      } 
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
      Sequence<Integer> portPath=context.getPortSession().getPort();
      for (Action action:actions)
      {
        Sequence<Integer> path
          =action.getTargetPath();
        if (portPath!=null)
        { path=path.subsequence(portPath.size());
        }
        
        
        dispatchProfiled
          ("ACTION"
          ,context
          ,new ActionMessage(action)
          ,component
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
    (Component component
    ,ServiceContext serviceContext
    )
   
  {
    if (serviceContext.getContentType()!=null)
    { 
      serviceContext.getResponse().setContentType
        (serviceContext.getContentType());
    }
    else
    { serviceContext.getResponse().setContentType
        ( component.getContentType());
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
    dispatchProfiled("RENDER",serviceContext,RenderMessage.INSTANCE,component,null);
    
    
  }    
  
  private void dispatchProfiled
      (String contextID,ServiceContext context,Message message,Component component,Sequence<Integer> path)
  {
    ProfilerAgent profilerAgent=context.getProfilerAgent();
    if (profilerAgent!=null)
    { 
      profilerAgent.enter
        ("UISequencer.dispatch:"+contextID, null);
//      log.fine("IN:"+contextID);
    }
    try
    { context.dispatch(message,component,path);
    }
    finally
    { 
      profilerAgent=context.getProfilerAgent();
      if (profilerAgent!=null)
      {
//        log.fine("OUT:"+contextID);
        profilerAgent.exit
          ("UISequencer.dispatch:"+contextID
          , null
          ,null
          );
      }
    }
  }
}
