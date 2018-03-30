//
// Copyright (c) 2015 Michael Toth
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

package spiralcraft.servlet.kit;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.ServletOutputStream;

import java.nio.charset.Charset;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import spiralcraft.net.mime.ContentTypeHeader;
import spiralcraft.net.mime.GenericHeader;
import spiralcraft.net.mime.MimeHeader;
import spiralcraft.net.mime.MimeHeaderMap;
import spiralcraft.util.string.StringUtil;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import spiralcraft.log.Level;
import spiralcraft.log.ClassLog;


/**
 * Implementation of HttpServletResponse for use by internal components to
 *   directly invoke functionality for utility or testing purposes.
 */
public class InternalHttpServletResponse
  implements HttpServletResponse
{
  
  private static final ClassLog _log
    =ClassLog.getInstance(InternalHttpServletResponse.class);

  static class OutputStream
    extends ServletOutputStream
  {
    private final ByteArrayOutputStream out=new ByteArrayOutputStream();
    
    @Override
    public void write(int b)
      throws IOException
    { out.write(b);
    }

    @Override
    public void write(byte[] b)
      throws IOException
    { out.write(b);
    }

    @Override
    public void write(byte[] b,int start,int len)
      throws IOException
    { out.write(b,start,len);
    }
    
    public void write(String s)
      throws IOException
    { out.write(StringUtil.asciiBytes(s));
    }
    
    public void reset()
    { out.reset();
    }

  }
  
  
  private final static String HDR_CONTENT_LENGTH = "Content-Length";
  private final static String HDR_CONTENT_TYPE = "Content-Type";
  private final static String HDR_LOCATION = "Location";

  private static final Charset UTF_8=Charset.forName("UTF-8");
  
  private ArrayList<Cookie> _cookies;
  private int _status;
  private String _reason;
  private boolean _sentHeaders=false;
  private OutputStream _outputStream;
  private boolean _shouldClose=true;
  private int _keepaliveSeconds=30;
  private Locale _locale;
  private PrintWriter _writer;
  private boolean debugAPI;
  private String contentType;
  private String characterEncoding="UTF-8";
  private int bufferSize=8192;
  private boolean committed;
  private MimeHeaderMap headerMap=new MimeHeaderMap();
  
  
  public InternalHttpServletResponse()
  { _outputStream=new OutputStream();
  }


  @Override
  public void reset()
  {
    if (debugAPI)
    { _log.fine("Resetting");
    }
    resetBuffer();
    if (_cookies!=null)
    { _cookies.clear();
    }
    _status=200;
    _reason=null;
    _sentHeaders=false;
    contentType=null;
    characterEncoding="UTF-8";
    headerMap.clear();
    
  }
  


  @Override
  public int getBufferSize()
  { return bufferSize;
  }

  @Override
  public void flushBuffer()
    throws IOException
  { 
    if (debugAPI)
    { _log.fine("Flushing buffer: "+_writer!=null?"writer+stream":"writer");
    }
    
    if (_writer!=null)
    { _writer.flush();
    }
    _outputStream.flush();
  }

  @Override
  public void setBufferSize(int bufferSize)
  {
    if (debugAPI)
    { _log.fine("Buffer size is "+bufferSize);
    }
    this.bufferSize=bufferSize;
  }

  @Override
  public boolean isCommitted()
  { 
    if (debugAPI)
    { _log.fine(committed?"COMMITTED":"not committed");
    }
    return committed;
  }

  @Override
  public void addCookie(Cookie cookie)
  {
    if (debugAPI)
    { _log.fine(""+cookie);
    }

    if (cookie==null)
    { throw new IllegalArgumentException("Cookie cannot be null");
    }
    
    if (_cookies==null)
    { _cookies=new ArrayList<Cookie>();
    }
    _cookies.add(cookie);
  }

  @Override
  public boolean containsHeader(String name)
  {     
    if (debugAPI)
    { _log.fine(name+" "+headerMap.getHeaders(name));
    }
    return headerMap.getHeaders(name)!=null;
  }

  @Override
  @Deprecated
  public String encodeRedirectUrl(String url)
  { 
    if (debugAPI)
    { _log.fine(url);
    }
    return url;
  }
  
  @Override
  @Deprecated
  public String encodeUrl(String url)
  { 
    if (debugAPI)
    { _log.fine(url);
    }
    return url;
  }

  @Override
  public String encodeRedirectURL(String url)
  { 
    if (debugAPI)
    { _log.fine(url);
    }
    return url;
  }
  
  @Override
  public String encodeURL(String url)
  { 
    if (debugAPI)
    { _log.fine(url);
    }
    return url;
  }

  @Override
  public void sendError(int code,String msg) 
    throws IOException
  {
    if (debugAPI)
    { _log.fine(code+" "+msg);
    }
    sendError(code,msg);
  }
      
  @Override
  public void sendError(int code) 
    throws IOException
  {
    if (debugAPI)
    { _log.fine(""+code);
    }

    String msg =  _statusMap.get(code);
    if (msg==null)
    { sendError(code,"Unknown Error");
    }
    else
    { sendError(code,msg);
    }
  }

  @Override
  public void sendRedirect(String location)
    throws IOException
  {
    if (debugAPI)
    { _log.fine(location);
    }

    setHeader(HDR_LOCATION,location);
    setStatus(SC_MOVED_TEMPORARILY);
    commitResponse();
    if (_writer!=null)
    { _writer.flush();
    }
    _outputStream.flush();
  }

  @Override
  public void setLocale(Locale locale)
  { 
    if (debugAPI)
    { _log.fine(""+locale);
    }
    _locale=locale;     
  }

  @Override
  public Locale getLocale()
  { 
    if (_locale==null)
    { _locale=Locale.getDefault();
    }
    if (debugAPI)
    { _log.fine(""+_locale);
    }
    return _locale;
  }

  @Override
  public void setStatus(int code)
  { 
    if (debugAPI)
    { _log.fine(""+code);
    }
    _status=code;
    _reason=_statusMap.get(code);
  }

  @Override
  @Deprecated
  public void setStatus(int code,String message)
  {
    if (debugAPI)
    { _log.fine(code+" "+message);
    }
    _status=code;
    _reason=message;
  }

  /**
   * Return the status code
   */
  public int getStatus()
  { 
    if (debugAPI)
    { _log.fine(""+_status);
    }
    return _status;
  }

  @Override
  public void setIntHeader(String name, int value)
  {
    headerMap.removeHeaders(name);
    addIntHeader(name,value);
  }

  @Override
  public void setDateHeader(String name, long date)
  {
    headerMap.removeHeaders(name);
    addDateHeader(name,date);
  }
  
  @Override
  public void addIntHeader(String name, int value)
  { addHeader(name, Integer.toString(value));
  }

  @Override
  public void addDateHeader(String name,long date)
  { addHeader(name, _headerDateFormat.format(new Date(date)));
  }

  @Override
  public void addHeader(String name, String value)
  { 
    if (debugAPI)
    { _log.fine(name+" = "+value);
    }
    headerMap.add(new GenericHeader(name,value));
  }

  @Override
  public void setHeader(String name, String value)
  {
    headerMap.removeHeaders(name);
    addHeader(name,value);
  }

  public String getHeader(String name)
  { 
    MimeHeader var=headerMap.getHeader(name);
    if (debugAPI)
    { _log.fine(name+" = "+(var!=null?var.getRawValue():"null"));
    }
    
    return var!=null?var.getRawValue():null;
  }

  @Override
  public void setContentType(String value)
  { 
    if (debugAPI)
    { _log.fine(value);
    }
    
    
    try
    {
      String contentType;
      String characterEncoding;
      
      ContentTypeHeader header=new ContentTypeHeader("Content-Type",value);
      characterEncoding=header.getParameter("charset");
      contentType=header.getFullType();
      this.contentType=contentType;
      if (characterEncoding!=null)
      { this.characterEncoding=characterEncoding;
      }
      
    }
    catch (IOException x)
    { _log.log(Level.WARNING,"Bad content type "+value,x);
    }
    
    setHeader
      (HDR_CONTENT_TYPE
      ,contentType
      +(characterEncoding!=null?";charset="+this.characterEncoding:"")
      );
  }
  
  @Override
  public String getContentType()
  { 
    if (debugAPI)
    { _log.fine(getHeader(HDR_CONTENT_TYPE));
    }
    return getHeader(HDR_CONTENT_TYPE);
  }
  
  @Override
  public void setCharacterEncoding(String value)
  {
    if (debugAPI)
    { _log.fine(value);
    }
    characterEncoding=value;
    if (contentType!=null)
    {
      contentType=StringUtil.discardAfter(contentType,';');

      setHeader
        (HDR_CONTENT_TYPE
        ,contentType
        +(characterEncoding!=null?";charset="+characterEncoding:"")
        );
    }
  }
  
  @Override
  public void setContentLength(int len)
  { 
    if (debugAPI)
    { _log.fine(""+len);
    }
    setHeader(HDR_CONTENT_LENGTH,Integer.toString(len));
  }

  public String getReason()
  { return _reason;
  }
  
  @Override
  public ServletOutputStream getOutputStream()
  { 
    if (debugAPI)
    { _log.fine(""+_outputStream);
    }
    return _outputStream;
  }

  @Override
  public PrintWriter getWriter()
  {
    if (_writer==null)
    { 
      Charset charset
        =characterEncoding!=null
        ?Charset.forName(characterEncoding)
        :null
        ;
        
      if (charset==null)
      { charset=UTF_8;
      }
      _writer=new PrintWriter(new OutputStreamWriter(_outputStream,charset));
    }
    if (debugAPI)
    { _log.fine(""+_writer);
    }
    return _writer;
  }

  @Override
  public String getCharacterEncoding()
  { 
    if (debugAPI)
    { _log.fine(characterEncoding);
    }
    return characterEncoding;
  }

  public boolean shouldClose()
  { return _shouldClose;
  }

  public int getKeepaliveSeconds()
  { return _keepaliveSeconds;
  }


  @Override
  public void resetBuffer()
  { 
    if (debugAPI)
    { _log.fine("Resetting buffer: sentHeaders="+_sentHeaders);
    }
    _sentHeaders=false;
  }

  void commitResponse()
  { committed=true;
  }
  
  //////////////////////////////////////////////////////////////////
  //
  // Private Static Members
  //
  //////////////////////////////////////////////////////////////////

  private final DateFormat _headerDateFormat
    =new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
  { _headerDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }


  private final static HashMap<Integer,String> _statusMap 
    = new HashMap<Integer,String>();
  static
  {
    try
    {
      Field[] fields
        = HttpServletResponse.class.getDeclaredFields();
      for (int i=0;i<fields.length; i++)
      {
        int mods = fields[i].getModifiers();
        if (Modifier.isFinal(mods)
            && Modifier.isStatic(mods)
            && fields[i].getType().equals(Integer.TYPE)
            && fields[i].getName().startsWith("SC_")
            )
        {
          _statusMap.put
            ((Integer) fields[i].get(null)
            ,fields[i]
              .getName()
              .substring(3) 
              .replace('_',' ')
            );
        }
      }              
    }
    catch (Exception x)
    { 
      _log.log(Level.WARNING,"Exception creating error map",x);
    }
  }

  static class Variable
  {
    public String name;
    public String value;

    public Variable(String name,String value)
    {
      this.name=name;
      this.value=value;
    }
    
   
  }




}
