//
// Copyright (c) 1998,2009 Michael Toth
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
package spiralcraft.servlet.vfs;

import javax.servlet.ServletException;

import spiralcraft.servlet.HttpServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.FileNotFoundException;

import spiralcraft.log.Level;


import spiralcraft.time.Clock;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.Path;
import spiralcraft.util.PathPattern;
import spiralcraft.util.string.StringUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.Resource;

import java.text.SimpleDateFormat;


import java.io.File;
import java.io.FilenameFilter;

import java.util.Date;

import java.io.OutputStream;
import java.io.InputStream;

import spiralcraft.net.http.Headers;
import spiralcraft.net.http.RangeHeader;

public class FileServlet
  extends HttpServlet
{
  

  private static final long serialVersionUID = 1L;
  
  { autoConfigure=true;
  }
  
  private int _bufferSize=8192;
  private boolean _permitDirListing=true;
  private String[] _defaultFiles={"index.html","index.htm","default.htm"};
  private SimpleDateFormat _fileDateFormat
    =new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
  private int defaultCacheSeconds;
  private PathPattern[] hiddenPaths
    =new PathPattern[] 
      {new PathPattern("**/.svn/")
      ,new PathPattern("**/CVS/")
      };
  
  
  public FileServlet()
  {
  }

  /**
   * <p>Specify how many seconds should expire before the client re-validates
   *   the cache. Sets the Expires and Cache-Control: max-age headers. A value
   *   of -1 turns off cache  headers.
   * </p>
   * @param seconds
   */
  public void setDefaultCacheSeconds(int seconds)
  { this.defaultCacheSeconds=seconds;
  }
  
  public void setHiddenPaths(PathPattern[] hiddenPaths)
  {  this.hiddenPaths=hiddenPaths;
  }
  
  public PathPattern[] getHiddenPaths()
  { return hiddenPaths;
  }
  
  
  @Override
  public void setInitParameter(String name,String value)
    throws ServletException
  {
    if (name.startsWith("default."))
    { _defaultFiles=ArrayUtil.append(_defaultFiles,value);
    }
    else
    { super.setInitParameter(name,value);
    }
  }

  @Override
  public void service(HttpServletRequest request,HttpServletResponse response)
    throws IOException,ServletException
  {
    if (debugLevel.canLog(Level.DEBUG))
    { log.log(Level.DEBUG,"Servicing request for "+request.getRequestURI());
    }
    
    if (hidden(request.getServletPath()))
    {
      send404(request,response);
      return;
    }
    
    String path
      =getServletConfig().getServletContext()
        .getRealPath(request.getServletPath());
    
    if (path==null)
    { 
      // Send an error, because the URI could not be translated into
      //   a real path (malformed URI or illegal path)
      response.sendError
        (400
        ,"<H1>Bad Request</H1>"
        +"Server could not translate '"+request.getRequestURI()+"'"
        );

      return;
      
    }

    File file=new File(path);
    if (debugLevel.canLog(Level.DEBUG))
    { log.log(Level.DEBUG,"File Servlet serving "+path);
    }
    
    if (request.getRequestURI().endsWith("/"))
    {
      if (!isDirectory(file))
      { 
        send404(request,response);
        return;
      }
      else
      {
        path=findDefaultFile(file);
        if (path!=null)
        { 
          if (request.getMethod().equals("GET"))
          { sendFile(request,response,path);
          }
          else if (request.getMethod().equals("HEAD"))
          { sendHead(request,response,path);
          }
          else if (request.getMethod().equals("PUT"))
          { putFile(request,response,path);
          }
          else
          { 
            // Method not allowed
            response.sendError(405);
          }
        }
        else
        { 
          if (_permitDirListing)
          { 
            if (request.getMethod().equals("GET"))
            { sendDirectory(request,response,file);
            }
            else
            { 
              // Method not allowed
              response.sendError(405);
            }
          }
          else
          {
            // Send an error, because we couldn't find the servlet,
            //   and defaulting to sending the file would be bad.
            response.sendError
              (403
              ,"<H1>Forbidden</H1>"
              +"You don't have permission to access "
              +request.getRequestURI()+" on this server.<P>"
              );
          }
          return;
        }
      }
    }
    else
    { 
      if (isDirectory(file))
      { 
        response.sendRedirect
          (response.encodeRedirectURL(request.getRequestURI()+"/"));
      }
      else if (request.getMethod().equals("GET"))
      { sendFile(request,response,path);
      }
      else if (request.getMethod().equals("HEAD"))
      { sendHead(request,response,path);
      }
      else if (request.getMethod().equals("PUT"))
      { putFile(request,response,path);
      }
      else
      { response.sendError(405);
      }
    }
  }



  //////////////////////////////////////////////////////////////////
  //
  // Private Methods
  //
  //////////////////////////////////////////////////////////////////




  /**
   * Returns the default file for a directory, or
   *   null if none of the default files can be found
   */
  private String findDefaultFile(File dir)
  {
    for (int i=0;i<_defaultFiles.length;i++)
    { 
      if (exists(dir,_defaultFiles[i]))
      { return new File(dir,_defaultFiles[i]).getPath();
      }
    }
    return null;
  }
  
  private boolean hidden(String pathString)
  {
    Path path=new Path(pathString,'/');
    if (hiddenPaths!=null)
    {
      for (PathPattern pattern:hiddenPaths)
      { 
        if (pattern!=null && pattern.matches(path))
        { return true;
        }
      }
    }
    return false;
  }

  private void putFile
    (HttpServletRequest request
    ,HttpServletResponse response
    ,String path
    )
    throws IOException
  {
    try
    {
      String contentLengthString=request.getHeader("Content-Length");
      if (contentLengthString==null)
      {
        response.sendError(411);
        return;
      }
      int contentLength=Integer.parseInt(contentLengthString);

      Resource resource=Resolver.getInstance().resolve(new File(path).toURI());
      OutputStream out=resource.getOutputStream();
      try
      {
        InputStream in=request.getInputStream();
        StreamUtil.copyRaw(in,out,16384,contentLength);
        out.flush();
        response.setStatus(201);
      }
      finally
      {
        if (out!=null)
        { out.close();
        }
      }
    }
    catch (IOException x)
    { 
      log.log(Level.SEVERE,"Error writing "+request.getRequestURI()+":"+x.toString());
      x.printStackTrace();
      response.sendError(500,"Error transferring file");
    }
  }

  private void setHeaders
    (HttpServletResponse response
    ,Resource resource
    )
    throws IOException
  {
    String contentType
      =getServletConfig().getServletContext().getMimeType
        (resource.getLocalName());
    if (contentType!=null)
    { response.setContentType(contentType);
    }

    response.setDateHeader
      (Headers.LAST_MODIFIED
      ,floorToSecond(resource.getLastModified())
      );
    setCacheHeaders(response);
  }
  
  private void setCacheHeaders
    (HttpServletResponse response
    )
  {
    if (defaultCacheSeconds>-1)
    {
      long now=Clock.instance().approxTimeMillis();
      response.setDateHeader
        (Headers.EXPIRES
        ,floorToSecond(now)+(defaultCacheSeconds*1000)
        );
      response.setHeader(Headers.CACHE_CONTROL,"max-age="+defaultCacheSeconds);
    }
  }
  
  private void send404(HttpServletRequest request,HttpServletResponse response)
    throws IOException
  {
    response.sendError
      (404
      ,"<H2>404 - Not Found</H2>The specified path, <STRONG>"
      +request.getRequestURI()
      +"</STRONG> could not be found on this server."
      );        
    
  }
  
  /**
   * Send the headers for the specified file to the client
   */
  private void sendHead
    (HttpServletRequest request
    ,HttpServletResponse response
    ,String path
    )
    throws IOException
  {
    try
    {
      Resource resource=Resolver.getInstance().resolve(new File(path).toURI());
      if (!resource.exists())
      { 
        send404(request,response);
        return;
        
      }
      long lastModified=floorToSecond(resource.getLastModified());     
      
      try
      { 
        long ifModifiedSince=request.getDateHeader(Headers.IF_MODIFIED_SINCE);
        if (ifModifiedSince>0 && lastModified<=ifModifiedSince)
        {
          // Send unchanged status because resource not modified.
          setCacheHeaders(response);
          response.setStatus(304);
          response.getOutputStream().flush();
          return;
        }
        else if (ifModifiedSince>0 && debugLevel.canLog(Level.DEBUG))
        { log.log(Level.DEBUG,"If-Modified-Since: "+ifModifiedSince+", lastModified="+lastModified);
        }
      }
      catch (IllegalArgumentException x)
      {
        log.log
          (Level.WARNING
          ,"Unrecognized date format in header- If-Modified-Since: "
          +request.getHeader(Headers.IF_MODIFIED_SINCE)
          );
      }      

      setHeaders(response,resource);
      
      long size=resource.getSize();
      if (size>0 && size<Integer.MAX_VALUE)
      { response.setContentLength((int) size);
      } 
      
      response.getOutputStream().flush();
    }
    catch (FileNotFoundException x)
    {
      response.sendError
        (404
        ,"<H2>404 - Not Found</H2>The specified URL, <STRONG>"
        +request.getRequestURI()
        +"</STRONG> could not be found on this server."
        );
    }
    catch (IOException x)
    { 
      if (!x.getMessage().equals("Broken pipe")
          && !x.getMessage().equals("Connection reset by peer")
          )
      {
        log.log
          (Level.WARNING
          ,"IOException retrieving "+path+": "+x.toString()
          );
      }

    }
  }
  
  private long floorToSecond(long timeInMs)
  { return (long) Math.floor((double) timeInMs/(double) 1000)*1000;
  }
  
  /**
   * Send the specified file to the client
   */
  private void sendFile
    (HttpServletRequest request
    ,HttpServletResponse response
    ,String path
    )
    throws IOException
  {
    // Standard file service
    // Simply send the disk file
    
    InputStream resourceInputStream=null;
    
    try
    {
      Resource resource=Resolver.getInstance().resolve(new File(path).toURI());
      long lastModified=floorToSecond(resource.getLastModified());
      try
      { 
        long ifModifiedSince=request.getDateHeader(Headers.IF_MODIFIED_SINCE);
        if (ifModifiedSince>0 && lastModified<=ifModifiedSince)
        {
          // Send unchanged status because resource not modified.
          setCacheHeaders(response);
          response.setStatus(304);
          response.getOutputStream().flush();
          return;
        }
        else if (ifModifiedSince>0 && debugLevel.canLog(Level.DEBUG))
        {
          log.log(Level.DEBUG,"If-Modified-Since: "
                    +ifModifiedSince+", lastModified="+lastModified);
        }
      }
      catch (IllegalArgumentException x)
      {
        log.log
          (Level.WARNING
          ,"Unrecognized date format in header- If-Modified-Since: "
          +request.getHeader(Headers.IF_MODIFIED_SINCE)
          );
      }      


      resourceInputStream
        =resource.getInputStream();

      setHeaders(response,resource);

      /**
       * Interpret range
       * XXX Process multiple range-specs in header
       */
      RangeHeader rangeHeader=null;
      String rangeSpec=request.getHeader(Headers.RANGE);
      if (rangeSpec!=null)
      { rangeHeader=new RangeHeader(rangeSpec);
      }
        
      int contentLength=(int) resource.getSize();
      if (rangeHeader!=null)
      { 
        resourceInputStream.skip(rangeHeader.getSkipBytes());
        
        // XXX Will be a problem for resources longer than MAXINT
        
        contentLength
          =(int) Math.max(0,resource.getSize()-rangeHeader.getSkipBytes());
        
        if (rangeHeader.getMaxBytes()>-1)
        { contentLength=Math.min(contentLength,rangeHeader.getMaxBytes());
        }
        
        response.setContentLength(contentLength);
        
        response.setStatus(206);
        response.setHeader
          (Headers.CONTENT_RANGE
          ,"bytes "
          +rangeHeader.getSkipBytes()
          +"-"
          +Math.min(resource.getSize()-1,rangeHeader.getLastByte())
          +"/"
          +resource.getSize()
          );
      }
      else
      { response.setContentLength(contentLength);
      }
      
      StreamUtil.copyRaw
        (resourceInputStream
        ,response.getOutputStream()
        ,_bufferSize
        ,contentLength
        );
      
      response.getOutputStream().flush();
      
    }
    catch (FileNotFoundException x)
    {
      response.sendError
        (404
        ,"<H2>404 - Not Found</H2>The specified URL, <STRONG>"
        +request.getRequestURI()
        +"</STRONG> could not be found on this server."
        );
    }
    catch (IOException x)
    { 
      if (!x.getMessage().equals("Broken pipe")
          && !x.getMessage().equals("Connection reset by peer")
          )
      {
        log.log
          (Level.WARNING
          ,"IOException retrieving "+path+": "+x.toString()
          );
      }

    }
    finally
    { 
      if (resourceInputStream!=null)
      { resourceInputStream.close();
      }
    }
  }


  private void sendDirectory
    (HttpServletRequest request
    ,HttpServletResponse response
    ,File dir
    )
    throws IOException
  {
    if (log.canLog(Level.DEBUG))
    { log.log(Level.DEBUG,"Listing "+dir.getPath());
    }

    String host=request.getHeader("Host");
    String uri=request.getRequestURI();
    if (!uri.endsWith("/"))
    { uri=uri.concat("/");
    }
    response.setContentType("text/html");
    
    StringBuffer out=new StringBuffer();
    out.append("<HTML><HEAD>\r\n");
    out.append("<TITLE>Index of ");
    out.append(uri);
    out.append("</TITLE>\r\n");
    out.append("</HEAD><BODY>\r\n");
    out.append("<H1>Index of ");
    out.append(uri);
    out.append("</H1>\r\n");
    out.append("<HR>\r\n");

    if (uri.length()>1)
    {
      out.append("<A href=\"");
      out.append(request.getScheme()+"://");
      out.append(host);
      String parent=new Path(uri,'/').parentPath().format("/");
      if (parent!=null)
      { out.append(parent);
      }
      out.append("\">Parent Directory</A>\r\n");
    }

    String[] dirs
      =dir.list
        (new FilenameFilter()
          {
            public boolean accept(File dir,String name)
            { return new File(dir,name).isDirectory();
            }
          }
        );


    out.append("<TABLE>");
    out.append("<TR>");
    out.append("<TH align=\"left\">Modified Date ("+_fileDateFormat.getTimeZone().getDisplayName()+")");
    out.append("</TH>");
    out.append("<TH align=\"left\">Size</TH>");
    out.append("<TH align=\"left\">Filename</TH>");
    out.append("</TR>");

    if (dirs!=null)
    {
      for (int i=0;i<dirs.length;i++)
      { 
        File subdir=new File(dir,dirs[i]);
        if (!hidden(subdir.toURI().getPath()))
        {
          out.append("<TR>");
          out.append("<TD align=\"left\"><TT>");
          out.append(_fileDateFormat.format(new Date(subdir.lastModified())));
          out.append("</TT></TD>");

          out.append("<TD>");
          out.append("</TD>");

          out.append("<TD><TT>");
          out.append("<A href=\"");
          out.append(request.getScheme()+"://");
          out.append(host);
          out.append(uri);
          out.append(dirs[i]);
          out.append("/\">");
          out.append(dirs[i]);
          out.append("/</A>");
          out.append("</TT></TD>");
          out.append("</TR>\r\n");
        }
      }
    }

    String[] files
      =dir.list
        (new FilenameFilter()
          {
            public boolean accept(File dir,String name)
            { return !(new File(dir,name).isDirectory());
            }
          }
        );

    if (files!=null)
    {
      for (int i=0;i<files.length;i++)
      { 
        File file=new File(dir,files[i]);
        if (!hidden(file.toURI().getPath()))
        {
          out.append("<TR>");

          out.append("<TD  align=\"left\"><TT>");
          out.append(_fileDateFormat.format(new Date(file.lastModified())));
          out.append("</TT></TD>");

          out.append("<TD align=\"right\"><TT>");
          out.append(file.length());
          out.append("</TT></TD>");

          out.append("<TD><TT>");
          out.append("<A href=\"");
          out.append(request.getScheme()+"://");
          out.append(host);
          out.append(uri);
          out.append(files[i]);
          out.append("\">");
          out.append(files[i]);
          out.append("</A>");
          out.append("</TT></TD>");
          out.append("</TR>\r\n");
        }
      }
    }

    out.append("</TABLE>");
    out.append("</BODY></HTML>");

    response.setContentLength(out.length());
    response.getOutputStream().write(StringUtil.asciiBytes(out.toString()));
    response.getOutputStream().flush();

  }

  private boolean isDirectory(File file)
  {
    // XXX Potentially real slow
    return file.isDirectory();
  }

  private boolean exists(File dir,String name)
  {
    // XXX Potentially real slow
    return new File(dir,name).exists();
  }
  

}
