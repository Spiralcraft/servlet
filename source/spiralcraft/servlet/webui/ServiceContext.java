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
 
import java.io.Writer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
//import java.nio.charset.Charset;

import java.util.LinkedList;
import java.util.List;

import spiralcraft.app.StateFrame;
import spiralcraft.servlet.PublicLocator;
import spiralcraft.servlet.kit.ContextAdapter;
import spiralcraft.servlet.webui.kit.PortSession;
import spiralcraft.text.html.URLDataEncoder;
import spiralcraft.textgen.EventContext;
import spiralcraft.util.Sequence;
import spiralcraft.util.URIUtil;
import spiralcraft.util.thread.ThreadLocalStack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;

import spiralcraft.net.http.VariableMap;
import spiralcraft.net.http.MultipartVariableMap;

import spiralcraft.vfs.StreamUtil;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;

/**
 * Provides webui components with the resources they need
 *   while handling actions and rendering output.
 * 
 * @author mike
 *
 */
public class ServiceContext
  extends EventContext
  implements UIContext
{
  public static final URI FOCUS_URI
    =URI.create("class:/spiralcraft/servlet/webui/ServiceContext");
  
  private static final ClassLog log
    =ClassLog.getInstance(ServiceContext.class);
  
  private static final Level debugLevel
    =ClassLog.getInitialDebugLevel(ServiceContext.class,null);
  
  private static final ThreadLocalStack<ServiceContext> threadContext
    =new ThreadLocalStack<ServiceContext>(true);
  
  public static final ServiceContext get()
  { return threadContext.get();
  }
  
  public static final String dataEncode(String data)
  { return URLDataEncoder.encode(data);
  }
  
//  private static final Charset UTF_8=Charset.forName("UTF-8");
  
  private PortSession portSession;
  private ContextAdapter servletContext;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private VariableMap post;
  private VariableMap query;
  private VariableMap form;
  private URI redirectURI;
  private List<String> queuedActions;
  private URI contextURI;
  private String contentType;
  private boolean outOfSync;
  private boolean initial;
  private PublicLocator publicLocator;
  
  
  public ServiceContext(Writer writer,boolean stateful,StateFrame frame)
  { 
    super(writer,stateful,frame);
    threadContext.push(this);
  }

  
  /**
   * Indicates that an old request has been resubmitted through a bookmark
   *   or a back button and that references to actions or controls may
   *   be stale. 
   * 
   * @return Whether this request is out of sync with the current state
   */
  public boolean getOutOfSync()
  { return outOfSync;
  }
  
  /**
   * Indicates that an old request has been resubmitted through a bookmark
   *   or a back button and that references to actions or controls may
   *   be stale. 
   * 
   */
  public void setOutOfSync(boolean outOfSync)
  { this.outOfSync=outOfSync;
  }
  
  /**
   * 
   * @return Whether this is the initial request of a conversation
   */
  public boolean getInitial()
  { return initial;
  }
  
  /**
   * Whether this is the initial request of a conversation
   * 
   */
  public void setInitial(boolean initial)
  { this.initial=initial;
  }
  
  
  @Override
  public String getContextInfo()
  { 
    HttpSession session=request.getSession(false);
    if (session!=null)
    { return "s:"+session.getId()+":"+portSession.getLocalURI();
    }
    else
    { return portSession.getLocalURI();
    }
  }
  
  /**
   * Release all resources
   */
  void release()
  {
    portSession=null;
    request=null;
    response=null;
    if (post!=null)
    { 
      post.clear();
      post=null;
    }
    if (query!=null)
    { 
      query.clear();
      query=null;
    }
    form=null;
    redirectURI=null;
    threadContext.pop();
  }
  
  /**
   * If not null, WebUI will respond to the client with an http redirect
   *   instead of rendering the page after the request processing phase
   *   has been completed.
   * 
   * @return The redirect URI in effect, if any.
   */
  public URI getRedirectURI()
  { return redirectURI;
  }
  
  /**
   * <p>Associate a resource session with this ServiceContext
   * </p>
   * 
   * <p>Called once at the start of every request
   * </p>
   * 
   * @param resourceSession The PortSession that stores data and
   *   objects for a user's session that are associated with the 
   *   containing WebUI user interface resource
   */
  public void setPortSession(PortSession resourceSession)
  { this.portSession=resourceSession;
  }  
    
  /**
   * 
   * @return The ResourceSession that stores data and
   *   objects for a user's session that are associated with the 
   *   containing WebUI user interface resource
   */
  @Override
  public PortSession getPortSession()
  { return portSession;
  }

  
  @Override
  public HttpServletRequest getRequest()
  { return request;
  }
  
  void setRequest(HttpServletRequest request)
    throws IOException,ServletException
  { 
    this.request=request;
    
    String queryString=request.getQueryString();
    if (queryString!=null && queryString.length()>0)
    { this.query=VariableMap.fromUrlEncodedString(queryString);
    }
    
    if (request.getContentLength()>0)
    {
      String contentType=request.getContentType();
      if (contentType.startsWith("application/x-www-form-urlencoded"))
      { 
        // String characterEncoding=request.getCharacterEncoding();
        String postString
          =StreamUtil.readAsciiString
            (request.getInputStream()
            ,request.getContentLength()
            );
        // XXX Pass character encoding to VariableMap
        this.post=VariableMap.fromUrlEncodedString(postString);
      }
      else if (contentType.startsWith("multipart/form-data"))
      {
        MultipartVariableMap map=new MultipartVariableMap();
        if (request.getCharacterEncoding()!=null)
        { map.setDefaultPartEncoding(request.getCharacterEncoding());
        }
        map.read
          (request.getInputStream()
          ,contentType
          ,request.getContentLength()
          );
        this.post=map;
        
      }
      else
      { 
        throw new ServletException
          ("Unrecognized content type: "+request.getContentType());
      }
    }

  }
  
  @Override
  public HttpServletResponse getResponse()
  { return response;
  }
  
  void setResponse(HttpServletResponse response)
  { this.response=response;
  }
  
  void setServletContext(ContextAdapter contextAdapter)
  { this.servletContext=contextAdapter;
  }
  
  public ContextAdapter getServletContext()
  { return servletContext;
  }
  
  
  /**
   * The URL that can be used to issue asynchronous requests back to this
   *   page. The URL reflects the current state of the conversation.
   *   
   * @return
   */
  @Override
  public String getAsyncURL()
  { return portSession.getAsyncURL();
  }
  
  public String registerAction(Action action)
  {
    String rawUrl=portSession.registerAction(action);
    return response.encodeURL(rawUrl);
  }

  public String registerPort(String stateId,Sequence<Integer> path)
  {
    String rawUrl=portSession.registerPort(stateId,path);
    return response.encodeURL(rawUrl);
  }
  
  @Override
  public String getAbsoluteBackLink()
  { return response.encodeURL(portSession.getAbsoluteBackLink(request));
  }
  
  /**
   * Return a state path relative to the current port
   * 
   * @return
   */
  public Sequence<Integer> getPortRelativePath()
  { 
    if (portSession.getPort()!=null)
    { return getState().getPath().subsequence(portSession.getPort().size());
    }
    else
    { return getState().getPath();
    }
  }
  
  public String secureLink(String relativePath)
  { 

    return getPublicLocator().secureLink(relativePath,request);
  }
  
  public String standardLink(String relativePath)
  { 
    return getPublicLocator().standardLink(relativePath,request);
  }
  
  @Override
  public PublicLocator getPublicLocator()
  {
    if (publicLocator==null)
    { publicLocator=PublicLocator.get(servletContext.getContext());
    }
    if (publicLocator==null)
    { 
      publicLocator
        =PublicLocator.deriveFromContext(servletContext.getContext(),request);
    }
    return publicLocator;
  }

  @Override
  public String getDataEncodedAbsoluteBackLink()
  { return URLDataEncoder.encode(portSession.getAbsoluteBackLink(request));
  }


  
  public Command<Void,Void,Void> redirectCommand(final String uriString)
  { 
    final URI uri=URI.create(uriString);
    return new CommandAdapter<Void,Void,Void>()
    {
      @Override
      public void run()
      { 
        try
        { redirect(uri);
        }
        catch (ServletException x)
        { setException(x);
        }
      }
    };
  }
  
  /**
   * <p>Directs that a redirect should occur before rendering (the page will
   *   still finish processing actions, or if preparing, will finish preparing)
   * </p>
   * 
   * @param uriStr A properly escaped string representation of the uri
   *   to redirect to
   * 
   */
  @Override
  public void redirect(String uriStr)
    throws URISyntaxException,ServletException
  { redirect(new URI(uriStr));
  }
  
  /**
   * <p>Directs that a redirect should occur before rendering (the page will
   *   still finish processing actions, or if preparing, will finish preparing)
   * </p>
   * 
   * @param rawURI 
   */
  @Override
  public void redirect(URI rawURI)
    throws ServletException
  {

    if (redirectURI!=null)
    { 
      log.warning
        ("Duplicate redirect "+rawURI+": Already redirecting to "+redirectURI);
    }
    
    
    
    // Combine the specified query with any encoded parameters
    
    String query=rawURI.getRawQuery();
    
    if (portSession==null)
    { throw new RuntimeException("??? null resource session "+this);
    }
    VariableMap sessionMap=portSession.getActionParameters();
    
    if (sessionMap!=null && !sessionMap.isEmpty())
    { 
      // We may need to merge the session parameters
      
      if (query==null || query.isEmpty())
      {
        // No query specified, just use the session map
        query=sessionMap.generateEncodedForm();
      }
      else
      { 
        // Query parameters override session map parameters
        VariableMap queryMap=VariableMap.fromUrlEncodedString(query);
        for (String key: sessionMap.keySet())
        { 
          if (!queryMap.containsKey(key))
          { queryMap.put(key,sessionMap.get(key));
          }
        }
        query=queryMap.generateEncodedForm();
      }
    }
    
    
    if (debugLevel.canLog(Level.DEBUG))
    {
      log.debug
        ("Encoding redirect to "+rawURI
        +"  session="+sessionMap.generateEncodedForm()
        +"  query="+query
        );
    }
    

    if (!rawURI.isAbsolute())
    { 
      URI requestURL=URI.create(request.getRequestURL().toString());
      
      // Make rawURI absolute
      rawURI= URI.create
        (requestURL.getScheme()
        +"://"
        +requestURL.getRawAuthority()
        +requestURL.getRawPath()
        )
        .resolve
        ( rawURI
        );
    }
      
    // Insert the combined query to complete the redirect URI
    redirectURI
      = URI.create
        (rawURI.getScheme()
        +"://"
        +rawURI.getRawAuthority()
        +(rawURI.getRawPath()!=null?rawURI.getRawPath():"")
        +(query!=null?"?"+query:"")
        +(rawURI.getRawFragment()!=null?"+"+rawURI.getRawFragment():"")
        );
      
    if (debugLevel.canLog(Level.DEBUG))
    { log.debug("Redirecting to "+redirectURI);
    }

    
  }
  
  
  /**
   * Provide a value for a request query variable. The variable is made part
   *   of all links generated for actions. 
   * 
   * @param name
   * @param values
   */
  public void setActionParameter(String name,List<String> values)
  { portSession.setActionParameter(name,values);
  }
  
  /**
   * Provide a value for a request query variable. The variable is made part
   *   of all links generated for actions. 
   * 
   * @param name
   * @param values
   */
  public void setActionParameter(String name,String ... values)
  { portSession.setActionParameter(name,values);
  }
      
  /**
   * Clear any parameters that will be added to the URL query string 
   *   when an action URI is rendered or a redirect is sent 
   */
  public void clearParameters()
  { portSession.clearParameters();
  }
  
  
  public boolean isSameReferer()
  { 
    String referer=request.getHeader("Referer");
    if (referer!=null && !referer.isEmpty())
    { 
      URI refererURI=URIUtil.trimToPath
        (URI.create(referer));
      URI requestURI=URIUtil.trimToPath
        (URI.create(request.getRequestURL().toString()));
      return requestURI.equals(refererURI);
    }
    else
    { return false;
    }
  }
  
  public URI getContextURI()
  { 
    if (contextURI==null)
    { 
      try
      {
        contextURI=new URI
          (request.getScheme()
          +"://"
          +request.getServerName()
          +(request.getServerPort()!=80?":"+request.getServerPort():"")
          +request.getContextPath()+"/"
          );
      }
      catch (URISyntaxException x)
      { log.log(Level.SEVERE,"Error assembling context URI",x);
      }
    }
    return contextURI;

  }
  
  
  @Override
  public VariableMap getPost()
  { return post;
  }
  
  @Override
  public VariableMap getQuery()
  { return query;
  }

  public void setForm(VariableMap form)
  { this.form=form;
  }
  
  public VariableMap getForm()
  { return form!=null?form:post;
  }
  
  /**
   * <p>Queue an Action for execution during this request processing
   *   cycle. This only has an effect when called during the action 
   *   handling phase of request processing.
   * </p>
   * 
   * @param actionName
   */
  public synchronized void queueAction(String actionName)
  { 
    if (debugLevel.canLog(Level.TRACE))
    { log.trace("Queued action "+actionName);
    }
    if (queuedActions==null)
    { queuedActions=new LinkedList<String>();
    }
    queuedActions.add(actionName);
  }
  
  public void setContentType(String contentType)
  { this.contentType=contentType;
  }
  
  public String getContentType()
  { return contentType;
  }
  
  
  
  /**
   * <p>Called by the UIServlet repeatedly during the action handling
   *   phase of request processing to complete the handling of all 
   *   indirectly triggered actions. 
   * </p>
   * 
   * @param actionName
   */
  public List<String> dequeueActions()
  { 
    List<String> list=queuedActions;
    queuedActions=null;
    return list;
  }

}
