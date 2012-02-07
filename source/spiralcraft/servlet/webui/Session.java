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

import spiralcraft.data.persist.PersistenceException;
import spiralcraft.data.persist.XmlBean;
import spiralcraft.lang.Focus;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>Contains the state of a UI (a set of resources in a directory mapped
 *  to a servlet) for a specific user over a period of interaction.
 * </p>
 * 
 * <p>The Session class may be extended for enhanced functionality.
 * </p>

 * <p>A Session is stored in the HttpSession.
 * </p>
 * 
 * <p>A Session contains a reference to the ResourceSession for each
 *   active WebUI resource in the ServletContext, mapped by the path
 *   of the resource relative to the ServletContext.
 * </p>
 */
public class Session
{

  
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
   * @param contextResource The VFS Container (directory) associated with 
   *   this session
   * @param defaultSessionTypeURI The type of session to create
   * @param parentFocus The Focus chain
   * @param create Whether to create a new Session if none exists
   * @return
   * @throws ServletException
   */
  @SuppressWarnings("unchecked") // Cast result from session.getAttribute()
  public static Session get
    (HttpServletRequest request
    ,Resource contextResource
    ,String sessionPath
    ,URI defaultSessionTypeURI
    ,Focus<?> parentFocus
    ,boolean create
    )
    throws ServletException,IOException
  {
    HttpSession session=request.getSession(create);
    if (session==null)
    { return null;
    }
    
    HashMap<String,XmlBean<Session>> sessionCache
      =(HashMap<String,XmlBean<Session>>) 
        session.getAttribute("spiralcraft.servlet.webui.sessionCache");
    
    if (sessionCache==null)
    { 
      synchronized (session)
      {
        sessionCache
          =(HashMap<String,XmlBean<Session>>) 
            session.getAttribute("spiralcraft.servlet.webui.sessionCache");  
        if (sessionCache==null)
        {
          sessionCache=new HashMap<String,XmlBean<Session>>();
          session.setAttribute
            ("spiralcraft.servlet.webui.sessionCache",sessionCache);
        }
      }
    }
    
    XmlBean<Session> uiSessionXmlAssembly
      =sessionCache.get(sessionPath);
    
    if (uiSessionXmlAssembly==null && create)
    { 
      uiSessionXmlAssembly
        =createUiSessionXmlBean(contextResource,defaultSessionTypeURI);
      uiSessionXmlAssembly.get().init(parentFocus);
      sessionCache.put(sessionPath, uiSessionXmlAssembly);
    }
    
    if (uiSessionXmlAssembly!=null)
    { return uiSessionXmlAssembly.get();
    }
    else
    { return null;
    }
  }
  
  private static XmlBean<Session> 
    createUiSessionXmlBean
      (Resource containerResource
      ,URI defaultSessionTypeURI
      )
  throws ServletException,IOException
  {
    if (containerResource.asContainer()==null)
    { containerResource=containerResource.getParent();
    }
    
    Resource sessionResource=null;
    if (containerResource!=null && containerResource.asContainer()!=null)
    {
      try
      { 
        sessionResource
          =containerResource.asContainer().getChild("Session.assy.xml");
      }
      catch (UnresolvableURIException x)
      { throw new ServletException(x.toString(),x);
      }
    }

    URI typeURI;
    if (sessionResource!=null && sessionResource.exists())
    { typeURI=containerResource.getURI().resolve("Session");
    }
    else
    { typeURI=defaultSessionTypeURI;
    }

    try
    { return new XmlBean<Session>(typeURI,null);
    }
    catch (PersistenceException x)
    { 
      throw new ServletException
      ("Error loading webui Session from ["+typeURI+"]:"+x,x);
    }

  }  
  
  
  // Holds a map from resource paths relative to the ServletContext
  //   to resource state.  
  private final HashMap<String,StateReference> stateMap
    =new HashMap<String,StateReference>();

  /**
   * 
   * @param parentFocus
   * @throws ServletException
   */
  public void init(Focus<?> parentFocus)
    throws ServletException
  {
  }
  
  /**
   * Get the ResourceSession associated with the UiComponent resource
   * 
   * @param component
   */
  public synchronized ResourceSession
    getResourceSession(RootComponent component)
  {
    StateReference ref=stateMap.get(component.getInstancePath());
    if (ref!=null && ref.component==component)
    { return ref.localSession;
    }
    else if (ref==null)
    {
      ref=new StateReference();
      stateMap.put(component.getInstancePath(),ref);
    }
    else
    {
      // there's a new component at that path- the old one can't exist
      // anymore. Components may implement multi-session capability internally.
      ref.component=component;
      ref.localSession=null;
    }
    return ref.localSession;
    
  }

  /**
   * Set the ResourceSession associated with the UiComponent resource
   * 
   * @param component
   */
  public synchronized void
    setResourceSession(RootComponent component,ResourceSession localSession)
  {
    StateReference ref=stateMap.get(component.getInstancePath());
    if (ref==null)
    { 
      ref=new StateReference();
      stateMap.put(component.getInstancePath(), ref);
    }
    ref.component=component;
    ref.localSession=localSession;
  }
  
}

class StateReference
{
  public ResourceSession localSession;
  public RootComponent component;
}
