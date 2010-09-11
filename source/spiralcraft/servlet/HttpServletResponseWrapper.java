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

  @Override
  public void addCookie(Cookie cookie)
  { delegate.addCookie(cookie);
  }

  @Override
  public boolean containsHeader(String name)
  { return delegate.containsHeader(name);
  }

  /**
   * @deprecated
   */
  @Override
  @SuppressWarnings("deprecation")
  @Deprecated
  public String encodeRedirectUrl(String url)
  { return delegate.encodeRedirectUrl(url);
  }
  
  /**
   * @deprecated
   */
  @Override
  @SuppressWarnings("deprecation")
  @Deprecated
  public String encodeUrl(String url)
  { return delegate.encodeUrl(url);
  }

  @Override
  public String encodeRedirectURL(String url)
  { return delegate.encodeRedirectURL(url);
  }
  
  @Override
  public String encodeURL(String url)
  { return delegate.encodeURL(url);
  }

  @Override
  public void sendError(int code,String msg) 
  	throws IOException
  { delegate.sendError(code,msg);
  }
      
  @Override
  public void sendError(int code) 
    throws IOException
  { delegate.sendError(code);
  }

  @Override
  public void sendRedirect(String location)
    throws IOException
  { delegate.sendRedirect(location);
  }

  @Override
  public void setLocale(Locale locale)
  { delegate.setLocale(locale);
  }

  @Override
  public Locale getLocale()
  { return delegate.getLocale();
  }


  @Override
  public void setStatus(int code)
  { delegate.setStatus(code);
  }


  /**
   * @deprecated
   */
  @Override
  @SuppressWarnings("deprecation")
  @Deprecated
  public void setStatus(int code,String message)
  { delegate.setStatus(code,message);
  }

  @Override
  public void setIntHeader(String name, int value)
  { delegate.setIntHeader(name,value);
  }

  @Override
  public void setDateHeader(String name, long date)
  { delegate.setDateHeader(name,date);
  }
  
  @Override
  public void addIntHeader(String name, int value)
  { delegate.addIntHeader(name,value);
  }

  @Override
  public void addDateHeader(String name,long date)
  { delegate.addDateHeader(name,date);
  }

  @Override
  public void addHeader(String name, String value)
  { delegate.addHeader(name,value);
  }

  @Override
  public void setHeader(String name, String value)
  { delegate.setHeader(name,value);
  }


  @Override
  public void setContentType(String value)
  { delegate.setContentType(value);
  }

  @Override
  public void setContentLength(int len)
  { delegate.setContentLength(len);
  }

  @Override
  public ServletOutputStream getOutputStream()
    throws IOException
  { return delegate.getOutputStream();
  }

  @Override
  public PrintWriter getWriter()
    throws IOException
  { return delegate.getWriter();
  }

  @Override
  public String getCharacterEncoding()
  { return delegate.getCharacterEncoding();
  }

  @Override
  public void flushBuffer()
    throws IOException
  { delegate.flushBuffer();
  }

  @Override
  public void setBufferSize(int bufferSize)
  { delegate.setBufferSize(bufferSize);
  }

  @Override
  public int getBufferSize()
  { return delegate.getBufferSize();
  }

  @Override
  public boolean isCommitted()
  { return delegate.isCommitted();
  }

  @Override
  public void reset()
  { delegate.reset();
  }

  @Override
  public void resetBuffer()
  { delegate.resetBuffer();
  }

  @Override
  public String getContentType()
  { return delegate.getContentType();
  }

  @Override
  public void setCharacterEncoding(String encoding)
  { delegate.setCharacterEncoding(encoding);
  }
}
