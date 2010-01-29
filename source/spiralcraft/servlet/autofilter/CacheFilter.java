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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import spiralcraft.time.Clock;


/**
 * Adds caching policy headers for resources handled by this filter
 * 
 * @author mike
 *
 */
public class CacheFilter
    extends AutoFilter
{

  private int seconds;

  public void setSeconds(int seconds)
  { this.seconds=seconds;
  }
  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException,ServletException
  {
    
    chain.doFilter(request,new CacheWrapper((HttpServletResponse) response));
  }

  public String getFilterType()
  { return "redirect";
  }

  
  
  class CacheWrapper
    extends HttpServletResponseWrapper
  {
    private int status;
    private long date;
  
    public CacheWrapper(HttpServletResponse response)
    { super(response);
    }
  
    @Override
    public void setDateHeader(String name,long value)
    {  
      if (name.equals("Date"))
      { this.date=value;
      }
      super.setDateHeader(name,value);
      updated();
    }
  
    @Override
    public void setStatus(int status)
    { 
      this.status=status;
      super.setStatus(status);
      updated();
    }
  
    private void updated()
    { 
      if (date!=0 && status!=0)
      {
        long expires=floorToSecond(Clock.instance().approxTimeMillis());
        setDateHeader("Expires",expires);
        setHeader("Cache-Control","max-age="+seconds);
      }
    }

    private long floorToSecond(long timeInMs)
    { return (long) Math.floor((double) timeInMs/(double) 1000)*1000;
    }  

  }
}

