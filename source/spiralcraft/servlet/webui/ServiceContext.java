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

import spiralcraft.textgen.EventContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
  
  public ServiceContext(Writer writer,boolean stateful)
  { super(writer,stateful);
  }
    
   void setLocalSession(LocalSession localSession)
  { this.localSession=localSession;
  }  
    
  public LocalSession getLocalSession()
  { return localSession;
  }
  
  public HttpServletRequest getRequest()
  { return request;
  }
  
  void setRequest(HttpServletRequest request)
  { this.request=request;
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
}
