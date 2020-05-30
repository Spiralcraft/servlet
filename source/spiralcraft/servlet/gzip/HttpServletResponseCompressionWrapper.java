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
package spiralcraft.servlet.gzip;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.servlet.kit.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * <p>Compresses a servlet response
 * </p>
 * 
 * @author mike
 *
 */
public class HttpServletResponseCompressionWrapper
  extends HttpServletResponseWrapper
{
  private static final ClassLog log
    =ClassLog.getInstance(HttpServletResponseWrapper.class);
  protected GzipServletOutputStream out=null;
  protected PrintWriter writer=null;
  protected boolean compress=false;
  private boolean contentLengthSet=false;
  private final GZipFilter filter;
  private final HttpServletRequest request;
  private Level logLevel=Level.INFO;
  boolean bypass;
  boolean contentTypeSet;

  public HttpServletResponseCompressionWrapper
    (HttpServletRequest request
    ,HttpServletResponse delegate
    ,GZipFilter filter
    )
  { 
    super(delegate);
    this.request=request;
    this.filter=filter;
  }

  void setLogLevel(Level logLevel)
  { this.logLevel=logLevel;
  }
  
  @Override
  public void setContentType(String contentType)
  { 
    super.setContentType(contentType);
    contentTypeSet=true;
    if (filter.shouldCompress(request,this))
    { 
      log.fine("Starting compression for "+request.getRequestURL()+" "+getContentType());
      startCompression();
    }
    else
    { 
      log.fine("Bypassing compression for "+request.getRequestURL()+" "+getContentType());
      bypass=true;
    }
  }
  
  private void startCompression()
  { 
    if (!compress)
    { 
      if (contentLengthSet)
      { log.warning("Content length set before compression enabled");
      }
      compress=true;
      delegate.setHeader("Content-Encoding","gzip");
      if (out!=null)
      { out.startCompressing();
      }
    }
    else
    { log.warning("compression already started");
    }
  }

  @Override
  public ServletOutputStream getOutputStream()
    throws IOException
  { 
    
    if (out==null)
    { 
      out=new GzipServletOutputStream(delegate.getOutputStream());
      if (compress)
      { out.startCompressing();
      }
    }
    log.fine("Created "+out);
    return out;
  }

  @Override
  public PrintWriter getWriter()
    throws IOException
  { 
    if (writer==null)
    { writer=new PrintWriter(getOutputStream());
    }
    return writer;
  }

  @Override
  public void flushBuffer() 
    throws IOException 
  {
    if (writer!=null)
    { writer.flush();
    }
    else if (out!=null)
    { out.flush();
    }
  }

  public void finish()
    throws IOException
  { 
    if (out!=null)
    { out.finish();
    }
  }

  @Override
  public void setContentLength(int length)
  { 
    if (!compress)
    { 
      log.fine("Allowing setContentLength "+length);
      delegate.setContentLength(length);
      contentLengthSet=true;
    }
  }
}
