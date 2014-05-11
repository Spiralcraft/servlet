//
//Copyright (c) 2014 Michael Toth
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
package spiralcraft.servlet.autofilter;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.parser.LiteralNode;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.Permission;
import spiralcraft.text.html.URLDataEncoder;
import spiralcraft.util.Path;

/**
 * Protects access to a resource, allowing access when a specified
 *   guard Expression<Boolean> returns true, and returning an
 *   alternate result otherwise.
 * 
 * @author mike
 *
 */
public class GuardCondition
  implements Contextual
{

  private Binding<String> messageX;
  private Binding<Boolean> guardX;
  private Binding<URI> redirectUriX;
  private Binding<Permission[]> permissionsX;
  private Channel<AuthSession> authSessionX;
  private Path[] bypassPaths;
  private Level logLevel=Level.INFO;
  private ClassLog log=ClassLog.getInstance(GuardCondition.class);
  private int responseCode=501;
  

  public void setBypassPaths(Path[] bypassPaths)
  { this.bypassPaths=bypassPaths;
  }
  
  public void setResponseCode(int responseCode)
  { this.responseCode=responseCode;
  }
  
  public void setMessageX(Binding<String> messageX)
  { this.messageX=messageX;
  } 
  
  public void setGuardX(Binding<Boolean> guardX)
  { this.guardX=guardX;
  }
  
  public void setRedirectUriX(Binding<URI> redirectUriX)
  { this.redirectUriX=redirectUriX;
  }
  
  public void setRedirectUri(URI redirectURI)
  { 
    this.redirectUriX
      =new Binding<URI>
       (Expression.<URI>create
         (new LiteralNode<URI>(redirectURI)));
  }
  
  public void setPermissionsX(Binding<Permission[]> permissionsX)
  { this.permissionsX=permissionsX;
  }

  
  public Focus<?> bind(Focus<?> focus)
    throws ContextualException
  {
    authSessionX=LangUtil.findChannel(AuthSession.class,focus);
    if (messageX!=null)
    { messageX.bind(focus);
    }
    if (redirectUriX!=null)
    { 
      redirectUriX.setTargetType(URI.class);
      redirectUriX.bind(focus);
    }
    if (guardX!=null)
    { 
      guardX.bind(focus);
    }
    if (permissionsX!=null)
    { permissionsX.bind(focus);
    }
    
    if (guardX==null && permissionsX==null)
    {
      throw new BindException
        ("GuardCondition must be configured with one or more of the following "
         +" properties: guardX,permissionX"
        );
    }
    return focus;
  }  
  
  private URI createRedirectURI(URI redirectURI,HttpServletRequest request)
  {
    String referer=request.getRequestURL().toString();
    URI redirect
      =URI.create
        (redirectURI.getPath()+"?referer="+URLDataEncoder.encode(referer));
    return redirect;
  }

  
  
  public boolean checkCondition
    (HttpServletRequest httpRequest
    ,HttpServletResponse response
    ,Path relativePath
    )
    throws IOException,ServletException
  {
    if (pathBypassed(httpRequest,relativePath))
    { return true;
    }
    
    Boolean passedTest=guardX!=null?(Boolean.TRUE.equals(guardX.get())):null;    
    
    if (passedTest==null || Boolean.TRUE.equals(passedTest) && (permissionsX!=null))
    {
      if (authSessionX==null)
      { passedTest=false;
      }
      else
      {
        AuthSession session=authSessionX.get();
        if (session==null)
        { passedTest=false;
        }
        else
        { 
          Permission[] permissions=permissionsX.get();
          if (permissions!=null)
          { passedTest=session.hasPermissions(permissions);
          }
        }
            
      }
    }
        
    
    if (!Boolean.TRUE.equals(passedTest))
    {
      if (logLevel.isDebug())
      { log.debug("GuardCondition rejected request");
      }
        
      URI redirectURI=redirectUriX!=null?redirectUriX.get():null;
      if (redirectURI!=null)
      { 
        if (logLevel.isFine())
        { log.fine("Setting up redirect to "+redirectURI);
        }
        response.sendRedirect
          (createRedirectURI(redirectURI,httpRequest).toString());
      }
      else if (messageX!=null)
      { 
  
        response.setStatus(responseCode);
        String message=messageX.get();
        if (message!=null)
        {
          response.setContentType("text/plain");
          response.setContentLength(message.length());
          response.getWriter().write(message);
          response.getWriter().flush();
        }
      }
      return false;
    }
    else
    { 
      
      if (logLevel.isFine())
      { log.debug("GuardFilter passed request");
      }
      return true;
    }
  }

  public String getFilterType()
  { return "guard";
  }

  private boolean pathBypassed(HttpServletRequest request,Path relativePath)
  { 
    if (bypassPaths!=null)
    {
      for (Path path:bypassPaths)
      { 
        if (relativePath.startsWith(path))
        { return true;
        }
      }
    }
    return false;
    
  }
}