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
package spiralcraft.servlet.autofilter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.autofilter.spi.FocusFilter;

/**
 * Protects access to a resource, allowing access when a specified
 *   guard Expression<Boolean> returns true, and returning an
 *   alternate result otherwise.
 * 
 * @author mike
 *
 */
public class GuardFilter
    extends AutoFilter
{

  private boolean bound=false;
  private Expression<String> messageX;
  private Expression<Boolean> guardX;
  private Channel<String> messageChannel;
  private Channel<Boolean> guardChannel;
  
  private int responseCode=501;
  
  {
    setGlobal(true);
    setPattern("*");
  }

  
  public void setResponseCode(int responseCode)
  { this.responseCode=responseCode;
  }
  
  public void setMessageX(Expression<String> messageX)
  { this.messageX=messageX;
  } 
  
  public void setGuardX(Expression<Boolean> guardX)
  { this.guardX=guardX;
  }
  
  public void bind(Focus<?> focus)
    throws BindException
  {
    
    if (messageX!=null)
    { 
      messageChannel=focus.bind(messageX);
      if (debug)
      { messageChannel.setDebug(debug);
      }
    }
    if (guardX!=null)
    { 
      guardChannel=focus.bind(guardX);
      if (debug)
      { guardChannel.setDebug(debug);
      }
    }
    else
    { throw new BindException("GuardFilter.guardX must be specified");
    }
    bound=true;
  }  
  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException,ServletException
  {
    
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    
    if (!bound)
    { 
      try
      { bind(FocusFilter.getFocusChain(httpRequest));
      }
      catch (BindException x)
      { throw new ServletException("Error binding ErrorFilter",x);
      }
    }
    
    Boolean val=guardChannel.get();
    if (!Boolean.TRUE.equals(val))
    {
      if (debug)
      { log.debug("GuardFilter rejected request");
      }
        
      HttpServletResponse httpResponse=(HttpServletResponse) response;
      httpResponse.setStatus(responseCode);
      

     
      if (messageChannel!=null)
      { 
        String message=messageChannel.get();
        if (message!=null)
        {
          httpResponse.setContentType("text/plain");
          httpResponse.setContentLength(message.length());
          response.getWriter().write(message);
          response.getWriter().flush();
        }
      }
    }
    else
    { 
      
      if (debug)
      { log.debug("GuardFilter passed request");
      }
      chain.doFilter(request,response);
    }
  }

  public String getFilterType()
  { return "guard";
  }

}