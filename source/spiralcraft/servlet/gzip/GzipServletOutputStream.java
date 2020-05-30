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

import spiralcraft.log.ClassLog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;

import java.util.zip.GZIPOutputStream;

/**
 * <p>A SevletOutputStream which uses Gzip compression
 * </p>
 * 
 * <p>Since references to this stream will be obtained before compression
 *   eligibility is determined, pass-through behavior will occur until
 *   compression is enabled.
 * </p>
 * 
 * @author mike
 *
 */
public class GzipServletOutputStream
  extends ServletOutputStream
{
  private static final ClassLog log
    =ClassLog.getInstance(GzipServletOutputStream.class);
  private ServletOutputStream _downStream;
  private OutputStream _out;
  private GZIPOutputStream _gzout;
  private boolean dirty=false;
  private boolean compressing;

  public GzipServletOutputStream(ServletOutputStream downStream)
    throws IOException
  { this._downStream=downStream;
  }

  void startCompressing()
  { compressing=true;
  }
  
  private void init()
    throws IOException
  {
    _gzout=new GZIPOutputStream(_downStream,true);
    _out=new BufferedOutputStream(_gzout);
    log.fine("Initialized streams");
  }

  @Override
  public final void write(final int data)
    throws IOException
  { 
    if (compressing)
    {
      if (_out==null)
      { init();
      }
      _out.write(data);
      dirty=true;
    }
    else
    { _downStream.write(data);
    }
  }

  @Override
  public final void write(final byte[] data)
    throws IOException
  { 
    if (compressing)
    {
      if (_out==null)
      { init();
      }
      _out.write(data);
      dirty=true;
    }
    else
    { _downStream.write(data);
    }
  }

  @Override
  public final void write(final byte[] data,final int start,final int len)
    throws IOException
  { 
    if (compressing)
    {
      if (_out==null)
      { init();
      }
      _out.write(data,start,len);
      dirty=true;
    }
    else
    { _downStream.write(data,start,len);
    }
  }

  @Override
  public void flush()
    throws IOException
  { 
    if (compressing)
    {
      if (_out!=null && dirty)
      { 
        log.fine("Flushing");
        _out.flush();
        dirty=false;
      }
      else
      { log.fine("Not dirty");
      }
    }
    else
    { _downStream.flush();
    }
  }

  public void finish()
    throws IOException
  {
    if (compressing)
    {
      if (dirty)
      { flush();
      }
      else 
      { log.fine("Not dirty");
      }
    }
  }
  
  @Override
  public void close()
    throws IOException
  { 
    if (compressing)
    {
      _out.close();
      _gzout.close();
    }
  }
}
