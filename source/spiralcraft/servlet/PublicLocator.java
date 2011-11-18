//
// Copyright (c) 2011 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>Provides information to construct an external reference to a servlet 
 *   application.
 * </p>  
 *   
 * <p>Usually stored as a ServletContext attribute named 
 *   spiralcraft.servlet.PublicLocator
 * </p>
 * 
 * @author mike
 *
 */
public class PublicLocator
{
  
  private final String serverName;
  private final Integer securePort;
  private final Integer standardPort;
  private final String basePath;
  
  
  public static final PublicLocator get(ServletContext context)
  { 
    return (PublicLocator) context.getAttribute
        (PublicLocator.class.getName());
  }
  
  public static final PublicLocator deriveFromContext
    (ServletContext context,HttpServletRequest request)
  { 
    String serverName=request.getServerName();
    Integer securePort
      =request.isSecure()
      ?request.getServerPort()
      :request.getServerPort()+(443-80)
      ;
    Integer standardPort
      =request.isSecure()
      ?request.getServerPort()-(443-80)
      :request.getServerPort();
    String basePath=context.getContextPath();
    return new PublicLocator(serverName,standardPort,securePort,basePath);
    
  }
  
  public PublicLocator
    (String serverName
    ,Integer standardPort
    ,Integer securePort
    ,String basePath
    )
  {
    this.serverName=serverName;
    this.securePort=securePort;
    this.standardPort=standardPort;
    this.basePath=basePath;
  }

  public final void set(ServletContext context)
  { 
    context.setAttribute
        (PublicLocator.class.getName(),this);
  }
    
  public int getSecurePort()
  { return securePort;
  }
  
  public int getStandardPort()
  { return standardPort;
  }
  
  public String getServerName()
  { return serverName;
  }
  
  public String getBasePath()
  { return basePath;
  }
  
  public String secureLink(String relativePath,HttpServletRequest request)
  { 
    if (securePort!=null && securePort<0)
    { 
      throw new UnsupportedOperationException
        ("This application does not provide a secure channel");
    }
    Integer port
      =securePort!=null
      ?securePort
      :request.isSecure() && request.getServerPort()!=443
      ?request.getServerPort()
      :null;
    
    return
      "https://"
      +(serverName!=null?serverName:request.getServerName())
      +(port==null?"":":"+port)
      +"/"
      +(basePath!=null && !basePath.isEmpty()?(basePath+"/"):"")
      +(relativePath!=null?relativePath:"");
  }
  
  public String standardLink(String relativePath,HttpServletRequest request)
  {
    if (standardPort!=null && standardPort<0 && securePort!=null && securePort>0)
    { return secureLink(relativePath,request);
    }
    Integer port
      =standardPort!=null
      ?standardPort
      :!request.isSecure() && request.getServerPort()!=80
      ?request.getServerPort()
      :null;
    
    return
        "http://"
        +(serverName!=null?serverName:request.getServerName())
        +(port==null?"":":"+port)
        +"/"
        +(basePath!=null && !basePath.isEmpty()?(basePath+"/"):"")
        +(relativePath!=null?relativePath:"");
    
  }
}
