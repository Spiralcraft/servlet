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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;

import spiralcraft.servlet.HttpServletResponseWrapper;

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

  protected GzipServletOutputStream out=null;
  protected PrintWriter writer=null;
  protected boolean compress=false;
  private boolean contentLengthSet=false;

  public HttpServletResponseCompressionWrapper(HttpServletResponse delegate)
  { super(delegate);
  }

  public void startCompression()
    throws IOException
  { 
    if (!compress)
    { 
      if (contentLengthSet)
      { throw new IOException("Content length set before compression enabled");
      }
      compress=true;
      delegate.setHeader("Content-Encoding","gzip");
      if (out!=null)
      { out.startCompression();
      }
    }
    else
    { System.out.println("compression already started");
    }
  }

  @Override
  public ServletOutputStream getOutputStream()
    throws IOException
  { 
    if (out==null)
    { 
      out=new GzipServletOutputStream(delegate);
      if (compress)
      { out.startCompression();
      }
    }
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
    else
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
      delegate.setContentLength(length);
      contentLengthSet=true;
    }
  }
}
