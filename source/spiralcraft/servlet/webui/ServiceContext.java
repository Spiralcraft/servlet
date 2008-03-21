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
import java.util.List;

import spiralcraft.textgen.EventContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import spiralcraft.net.http.VariableMap;

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

  private ResourceSession resourceSession;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private VariableMap post;
  private VariableMap query;
  private CommandProcessor commandProcessor;
  
  public ServiceContext(Writer writer,boolean stateful)
  { super(writer,stateful);
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

  void setPost(VariableMap post)
  { this.post=post;
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
      if (request.getContentType().equals("application/x-www-form-urlencoded"))
      { 
        String postString
          =StreamUtil.readAsciiString
            (request.getInputStream(),request.getContentLength());
        this.post=VariableMap.fromUrlEncodedString(postString);
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
