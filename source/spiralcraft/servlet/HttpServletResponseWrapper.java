//
// Copyright (c) 1998,2008 Michael Toth
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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.ServletOutputStream;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Locale;

/**
 * 
 * <p>Wraps an HttpServletResponse to provide modified functionality to or
 *   intercept calls from downstream components.
 * </p>
 *   
 * @author mike
 *
 */
public class HttpServletResponseWrapper
  implements HttpServletResponse
{

  protected final HttpServletResponse delegate;

  public HttpServletResponseWrapper(HttpServletResponse delegate)
  { this.delegate=delegate;
  }

  public void addCookie(Cookie cookie)
  { delegate.addCookie(cookie);
  }

  public boolean containsHeader(String name)
  { return delegate.containsHeader(name);
  }

  /**
   * @deprecated
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public String encodeRedirectUrl(String url)
  { return delegate.encodeRedirectUrl(url);
  }
  
  /**
   * @deprecated
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public String encodeUrl(String url)
  { return delegate.encodeUrl(url);
  }

  public String encodeRedirectURL(String url)
  { return delegate.encodeRedirectURL(url);
  }
  
  public String encodeURL(String url)
  { return delegate.encodeURL(url);
  }

  public void sendError(int code,String msg) 
  	throws IOException
  { delegate.sendError(code,msg);
  }
      
  public void sendError(int code) 
    throws IOException
  { delegate.sendError(code);
  }

  public void sendRedirect(String location)
    throws IOException
  { delegate.sendRedirect(location);
  }

  public void setLocale(Locale locale)
  { delegate.setLocale(locale);
  }

  public Locale getLocale()
  { return delegate.getLocale();
  }

  /**
   * @deprecated
   */
  @Deprecated
  public void setStatus(int code)
  { delegate.setStatus(code);
  }


  /**
   * @deprecated
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public void setStatus(int code,String message)
  { delegate.setStatus(code,message);
  }

  public void setIntHeader(String name, int value)
  { delegate.setIntHeader(name,value);
  }

  public void setDateHeader(String name, long date)
  { delegate.setDateHeader(name,date);
  }
  
  public void addIntHeader(String name, int value)
  { delegate.addIntHeader(name,value);
  }

  public void addDateHeader(String name,long date)
  { delegate.addDateHeader(name,date);
  }

  public void addHeader(String name, String value)
  { delegate.addHeader(name,value);
  }

  public void setHeader(String name, String value)
  { delegate.setHeader(name,value);
  }


  public void setContentType(String value)
  { delegate.setContentType(value);
  }

  public void setContentLength(int len)
  { delegate.setContentLength(len);
  }

  public ServletOutputStream getOutputStream()
    throws IOException
  { return delegate.getOutputStream();
  }

  public PrintWriter getWriter()
    throws IOException
  { return delegate.getWriter();
  }

  public String getCharacterEncoding()
  { return delegate.getCharacterEncoding();
  }

  public void flushBuffer()
    throws IOException
  { delegate.flushBuffer();
  }

  public void setBufferSize(int bufferSize)
  { delegate.setBufferSize(bufferSize);
  }

  public int getBufferSize()
  { return delegate.getBufferSize();
  }

  public boolean isCommitted()
  { return delegate.isCommitted();
  }

  public void reset()
  { delegate.reset();
  }

  public void resetBuffer()
  { delegate.resetBuffer();
  }
}
