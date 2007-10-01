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

import spiralcraft.textgen.EventContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import spiralcraft.net.http.VariableMap;

import spiralcraft.vfs.StreamUtil;

/**
 * Provides webui components with the resources they need
 *   while handling actions and rendering output
 * 
 * @author mike
 *
 */
public class ServiceContext
  extends EventContext
{

  private LocalSession localSession;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private VariableMap post;
  private VariableMap query;
  
  public ServiceContext(Writer writer,boolean stateful)
  { super(writer,stateful);
  }
    
  void setLocalSession(LocalSession localSession)
  { this.localSession=localSession;
  }  
    
  void setPost(VariableMap post)
  { this.post=post;
  }

  public LocalSession getLocalSession()
  { return localSession;
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
    String rawUrl=localSession.registerAction(action,preferredName);
    return response.encodeURL(rawUrl);
  }
  
  public VariableMap getPost()
  { return post;
  }
  
  public VariableMap getQuery()
  { return query;
  }

}
