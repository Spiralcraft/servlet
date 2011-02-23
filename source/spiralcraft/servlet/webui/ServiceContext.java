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

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.StateFrame;
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
import spiralcraft.command.CommandProcessor;

/**
 * Provides webui components with the resources they need
 *   while handling actions and rendering output.
 * 
 * @author mike
 *
 */
public class ServiceContext
  extends EventContext
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
  
//  private static final Charset UTF_8=Charset.forName("UTF-8");
  
  private ResourceSession resourceSession;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private VariableMap post;
  private VariableMap query;
  private CommandProcessor commandProcessor;
  private URI redirectURI;
  private List<String> queuedActions;
  private URI contextURI;
  private String contentType;
  private boolean outOfSync;
  private boolean initial;
  
  
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
  public String getLogPrefix()
  { 
    HttpSession session=request.getSession(false);
    if (session!=null)
    { return "s:"+session.getId()+":"+resourceSession.getLocalURI();
    }
    else
    { return resourceSession.getLocalURI();
    }
  }
  
  /**
   * Release all resources
   */
  void release()
  {
    resourceSession=null;
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
    commandProcessor=null;
    redirectURI=null;
    threadContext.pop();
  }
  
  URI getRedirectURI()
  { return redirectURI;
  }
  
  /**
   * <p>Associate a resource session with this ServiceContext
   * </p>
   * 
   * <p>Called once at the start of every request
   * </p>
   * 
   * @param resourceSession The ResourceSession that stores data and
   *   objects for a user's session that are associated with the 
   *   containing WebUI user interface resource
   */
  void setResourceSession(ResourceSession resourceSession)
  { this.resourceSession=resourceSession;
  }  
    
  /**
   * 
   * @return The ResourceSession that stores data and
   *   objects for a user's session that are associated with the 
   *   containing WebUI user interface resource
   */
  public ResourceSession getResourceSession()
  { return resourceSession;
  }

  
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
  
  public HttpServletResponse getResponse()
  { return response;
  }
  
  void setResponse(HttpServletResponse response)
  { this.response=response;
  }
  
  /**
   * The URL that can be used to issue asynchronous requests back to this
   *   page. The URL reflects the current state of the conversation.
   *   
   * @return
   */
  public String getAsyncURL()
  { return resourceSession.getAsyncURL();
  }
  
  public String registerAction(Action action)
  {
    String rawUrl=resourceSession.registerAction(action);
    return response.encodeURL(rawUrl);
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
   * @param rawURI 
   */
  public void redirect(URI rawURI)
    throws ServletException
  {

    if (redirectURI!=null)
    { 
      throw new ServletException
        ("Duplicate redirect "+rawURI+": Already redirecting to "+redirectURI);
    }
    
    
    
    // Combine the specified query with any encoded parameters
    
    String query=rawURI.getRawQuery();
    
    if (resourceSession==null)
    { throw new RuntimeException("??? null resource session "+this);
    }
    String encodedParameters=resourceSession.getEncodedParameters();
    
    if (encodedParameters!=null)
    { 
      if (query==null || query.isEmpty())
      { query=encodedParameters;
      }
      else
      { query=query+"&"+encodedParameters;
      }
    }
    
    if (debugLevel.canLog(Level.DEBUG))
    {
      log.debug
        ("Encoding redirect to "+rawURI
        +"  parameters="+encodedParameters
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
  { resourceSession.setActionParameter(name,values);
  }
  
  /**
   * Provide a value for a request query variable. The variable is made part
   *   of all links generated for actions. 
   * 
   * @param name
   * @param values
   */
  public void setActionParameter(String name,String ... values)
  { resourceSession.setActionParameter(name,values);
  }
      
  /**
   * Clear any parameters that will be added to the URL query string 
   *   when an action URI is rendered or a redirect is sent 
   */
  public void clearParameters()
  { resourceSession.clearParameters();
  }
  
  /**
   * @param commandProcessor The CommandProcessor associated with this 
   *    context- called by components that wish to supply a
   *    a CommandProcessor.
   */
  public void setCommandProcessor(CommandProcessor commandProcessor)
  { this.commandProcessor=commandProcessor;
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
  
  /**
   * 
   * @return The CommandProcessor associated with this context
   */
  public CommandProcessor getCommandProcessor()
  { return commandProcessor;
  }
  
  public VariableMap getPost()
  { return post;
  }
  
  public VariableMap getQuery()
  { return query;
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
  List<String> dequeueActions()
  { 
    List<String> list=queuedActions;
    queuedActions=null;
    return list;
  }

}
