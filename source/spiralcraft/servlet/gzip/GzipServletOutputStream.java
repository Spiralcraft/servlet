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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;

import java.util.zip.GZIPOutputStream;

/**
 * <p>A SevletOutputStream which uses Gzip compression
 * </p>
 * 
 * @author mike
 *
 */
public class GzipServletOutputStream
  extends ServletOutputStream
{

  private ServletOutputStream _downStream;
//  private HttpServletResponse _response;
  private OutputStream _out;
  private GZIPOutputStream _gzout;
  private boolean _shouldCompress;

  public GzipServletOutputStream(HttpServletResponse response)
    throws IOException
  { 
//    _response=response;
    _downStream=response.getOutputStream();
  }

  public final void startCompression()
  { _shouldCompress=true;
  }

  private void initCompression()
    throws IOException
  {
    if (_out==null)
    { 
      _gzout=new GZIPOutputStream(_downStream);
      _out=new BufferedOutputStream(_gzout);
    }
    else
    { throw new IOException("Cannot start compression when output already started");
    }
  }

  @Override
  public final void write(final int data)
    throws IOException
  { 
    final byte[] bytes={(byte) data};
    write(bytes,0,1);
  }

  @Override
  public final void write(final byte[] data,final int start,final int len)
    throws IOException
  { 
    if (_out==null)
    { 
      if (_shouldCompress)
      { initCompression();
      }
      else
      { _out=_downStream;
      }
    }
    _out.write(data,start,len);
  }

  
  @Override
  public void write(byte[] data)
    throws IOException
  { write(data,0,data.length);
  }

  @Override
  public void flush()
    throws IOException
  { 
    if (_out!=null)
    { _out.flush();
    }
  }

  public void finish()
    throws IOException
  {
    if (_gzout!=null)
    { _gzout.finish();
    }
  }
  
  @Override
  public void close()
    throws IOException
  { _gzout.close();
  }
}
