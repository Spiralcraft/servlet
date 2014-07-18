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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.autofilter.PathContext;
import spiralcraft.servlet.kit.ContextAdapter;
import spiralcraft.servlet.kit.HttpFocus;
import spiralcraft.servlet.kit.UIResourceMapping;
import spiralcraft.servlet.webui.kit.PortSession;
import spiralcraft.servlet.webui.kit.UISequencer;
import spiralcraft.ui.NavContext;
import spiralcraft.util.Sequence;
import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Package;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;


import spiralcraft.app.StateFrame;
import spiralcraft.common.ContextualException;

/**
 * <p>Manages a set of WebUI interfaces and their session state 
 * </p>
 * 
 * 
 */
public class UIService
  implements Contextual
{

  private static final ClassLog log
    =ClassLog.getInstance(UIService.class);
  private static final Level logLevel
    =ClassLog.getInitialDebugLevel(UIService.class,null);
  
  
  private final ContextAdapter context;
  
  private UICache uiCache;
  private String contextRelativePath;
  private final HashMap<String,UIResourceMapping> uiResourceMap
    =new HashMap<>();
  private final HashSet<String> bypassSet
    =new HashSet<String>();
  
  private URI defaultSessionTypeURI
    =URI.create("class:/spiralcraft/servlet/webui/Session");
  
  private HttpFocus<?> httpFocus;
  @SuppressWarnings("rawtypes")
  private Focus<NavContext> navContextFocus;
  private Focus<PathContext> pathContextFocus;
  private Channel<UIService> selfChannel
    =new SimpleChannel<UIService>(this,true);
  
  private final UISequencer sequencer=new UISequencer();
  
  public UIService(ContextAdapter context,String contextRelativePath)
  { 
    this.context=context;
    this.contextRelativePath=contextRelativePath;
  }
  
  @Override
  public Focus<?> bind(Focus<?> focusChain)
    throws BindException
  {
    focusChain=focusChain.chain(selfChannel);
    httpFocus=new HttpFocus<Void>(focusChain);
    focusChain=httpFocus;
    uiCache=new UICache(focusChain,context);
    navContextFocus
      =LangUtil.findFocus(NavContext.class,focusChain);
    pathContextFocus
      =LangUtil.findFocus(PathContext.class,focusChain);
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
  
  /**
   * <p>Find the component that services the provided context-relative 
   *   request path, if one exists
   * </p>
   * 
   * @param relativePath
   * @return
   * @throws ContextualException
   * @throws IOException
   * @throws ServletException
   */
  public RootComponent findComponent(String relativePath)
    throws ContextualException,IOException,ServletException
  {

    if (bypassSet.contains(relativePath))
    { return null;
    }
    
    UIResourceMapping resource=uiResourceMap.get(relativePath);
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
    
    return getRootComponent(resource.resource,resource.mappedPath);
  }
  
  /**
   *   
   * <p>Find a code resource that generates a component for the
   *   specified context-relative request path
   * </p>
   *
   * @param relativePath
   * @return
   * @throws IOException
   * @throws ServletException
   */
  protected UIResourceMapping findUIResource(String relativePath)
    throws IOException, ServletException
  {
    
    UIResourceMapping resource=null;
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
          // Ignore any resource that doesn't end in .webui that exists
          //   in the standard www document tree. This will be handled by
          //   the caller in some other way as it is not a webui resource.
          return null;
        }
        else
        { 
          // Search for an appropriate .webui generator for a non-existent 
          //   static resource
          relativeResourcePath=relativePath+".webui";
        }
      }
    }
    else
    { relativeResourcePath=relativePath;
    }
    
    if (resource==null) 
    { 
      // Search for the .webui file in the standard www document tree
      resource
        =UIResourceMapping.forResource
          (relativePath
          ,context.getResource(relativeResourcePath)
          );
    }
    
//    if (resource==null)
//    { 
//      // XXX: This is dangerous. Find some way to mark content as
//      //   executable, because this could point to user upload paths.
//      resource=Package.findResource("context:"+relativeResourcePath);
//    }
    
    
    if (resource==null)
    { 
      resource=
        UIResourceMapping.forResource
          (relativePath
          ,Package.findResource("context://code"+relativeResourcePath)
          );
    }

    if (resource==null && pathContextFocus!=null)
    { 
      // Look in the PathContext object
      PathContext pathContext=pathContextFocus.getSubject().get();
      if (pathContext!=null)
      {
        
        resource=pathContext.uiResourceForRequest();
        
        if (resource==null)
        {
          String pathContextRelativePath
            =pathContext.relativize(relativeResourcePath);
          resource=
            UIResourceMapping.forResource
              (relativePath
              ,pathContext.resolveCode(pathContextRelativePath)
              );
          if (logLevel.isDebug())
          { 
            log.fine
              ("PathContext in "+pathContext.getAbsolutePath()
              +" with code at "+pathContext.getEffectiveCodeBaseURI()
              +" .resolveCode("+pathContextRelativePath+") returned "
              +resource
              );
          }
        }
      }
    }
    
    if (resource==null)
    {
      // Fall back to data-driven UI
      //
      // XXX This might be handled by the PathContext in the future
      NavContext<?,?> navContext=getNavContext();
    
      if (navContext!=null)
      {
        URI viewResourceURI=navContext.getViewResourceURI();
        if (viewResourceURI!=null)
        {
          if (viewResourceURI.isAbsolute())
          { 
            resource=
              UIResourceMapping.forResource
                (relativePath
                ,Resolver.getInstance().resolve(viewResourceURI)
                );
          }
          else
          { 
            UIResourceMapping.forResource
              (relativePath
              ,context.getResource(viewResourceURI.getPath())
              );
            
          }

          if (resource!=null && resource.resource.asContainer()!=null)
          {
            relativeResourcePath
              =navContext.getUnresolvedPath().format("/");
            Container container=resource.resource.asContainer();
            
            resource=
              UIResourceMapping.forResource
                (relativePath
                ,Resolver.getInstance().resolve
                  (URIUtil.ensureTrailingSlash(container.getURI())
                    .resolve(relativeResourcePath+".webui")
                  )
                );
            
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
              session.setResourceSession(component,localSession);
            }
          }
        }
        
        // Set the URI per-reuqest because the data components in the URI may
        //   change
        localSession.setLocalURI(request.getRequestURI());

        // Set up the ServiceContext with the last frame used.
        serviceContext
          =new ServiceContext
          (response.getWriter(),true,localSession.currentFrame());

        serviceContext.setRequest(request);
        serviceContext.setResponse(response);
        serviceContext.setServletContext(context);
        
        
        VariableMap query=serviceContext.getQuery();
        
        String port
          =query!=null
          ?query.getFirst("port")
          :null;

        if (port!=null && !port.isEmpty())
        { callPort(serviceContext,component,localSession,port);
        }
        else
        { sequencer.service(serviceContext,component,localSession);
        }
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
        sequencer.service(serviceContext,component,localSession);

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
  
  private void callPort
    (ServiceContext serviceContext
    ,Component component
    ,PortSession localSession
    ,String port
    )
  {
    HttpServletResponse response=serviceContext.getResponse();
    if (localSession.getState()==null)
    {
      
      log.warning
        ("No state found on port request for "
        +localSession.getLocalURI()
        );
      response.setStatus(500);
      try
      {
        response.getWriter().flush();
        response.flushBuffer();
      }
      catch (IOException x)
      {
      }
    }
    
    serviceContext.setState(localSession.getState());
    Sequence<Integer> path=localSession.getPort(port);
    if (path!=null)
    { serviceContext.dispatch(PortMessage.INSTANCE,component,path);
    }
    else
    { serviceContext.getResponse().setStatus(404);
    }
    
  }
}



