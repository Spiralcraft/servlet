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

import java.util.List;

import spiralcraft.textgen.EventContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import spiralcraft.log.ClassLogger;

import spiralcraft.net.http.VariableMap;
import spiralcraft.net.http.MultipartVariableMap;

import spiralcraft.vfs.StreamUtil;

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
  private static final ClassLogger log
    =ClassLogger.getInstance(ServiceContext.class);
  
  private ResourceSession resourceSession;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private VariableMap post;
  private VariableMap query;
  private CommandProcessor commandProcessor;
  private URI redirectURI;
  
  public ServiceContext(Writer writer,boolean stateful)
  { super(writer,stateful);
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
    
  }
  
  URI getRedirectURI()
  { return redirectURI;
  }
  
  /**
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
      if (contentType.equals("application/x-www-form-urlencoded"))
      { 
        String postString
          =StreamUtil.readAsciiString
            (request.getInputStream(),request.getContentLength());
        this.post=VariableMap.fromUrlEncodedString(postString);
      }
      else if (contentType.startsWith("multipart/form-data"))
      {
        MultipartVariableMap map=new MultipartVariableMap();
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
  
  public String registerAction(Action action,String preferredName)
  {
    String rawUrl=resourceSession.registerAction(action,preferredName);
    return response.encodeURL(rawUrl);
  }
  

  /**
   * Directs that a redirect should occur at the next possible opportunity
   *   before rendering.
   * 
   * @param rawUrl
   */
  public void redirect(URI rawURI)
    throws ServletException
  {
    // XXX Clean up URI reconstruction code
    if (redirectURI!=null)
    { 
      throw new ServletException
        ("Duplicate redirect "+rawURI+": Already redirecting to "+redirectURI);
    }
    
    URI requestURL=URI.create(request.getRequestURL().toString());
    
    String query=rawURI.getQuery();
    
    String encodedParameters=resourceSession.getEncodedParameters();
    
    if (encodedParameters!=null)
    { 
      if (query==null)
      { query="?"+encodedParameters;
      }
      else
      { query="?"+query+"&"+encodedParameters;
      }
    }
    else
    {
      if (query!=null)
      { query="?"+query;
      }
      
    }
    
    log.fine
      ("Encoding redirect to "+rawURI
      +"  parameters="+encodedParameters
      +"  query="+query
      );
    
    
    try
    {
      if (rawURI.isAbsolute())
      {
        redirectURI= new URI
          (rawURI.getScheme()
          ,rawURI.getUserInfo()
          ,rawURI.getHost()
          ,rawURI.getPort()
          ,rawURI.getPath()
          ,query
          ,rawURI.getFragment()
          );
      }
      else
      {
        redirectURI = new URI
          (requestURL.getScheme()
            ,requestURL.getUserInfo()
            ,requestURL.getHost()
            ,requestURL.getPort()
            ,requestURL.getPath()
            ,""
            ,""
          )
          .resolve
          ( rawURI
          );

        log.fine(redirectURI.toString());
        if (query!=null && encodedParameters!=null)
        { redirectURI=URI.create(redirectURI.toString()+"&"+encodedParameters);
        }
        log.fine(redirectURI.toString());
      }
    }
    catch (URISyntaxException x)
    { throw new ServletException("Error encoding redirect to "+rawURI,x);
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

  
}
