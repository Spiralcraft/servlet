//
// Copyright (c) 1998,2010 Michael Toth
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

import spiralcraft.servlet.kit.HttpServlet;

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
import spiralcraft.vfs.ResourceFilter;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.Resource;

import spiralcraft.vfs.Container;
import spiralcraft.vfs.UnresolvableURIException;

import java.text.SimpleDateFormat;

import java.io.File;
//import java.io.FilenameFilter;

import java.util.Date;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import spiralcraft.net.http.Headers;
import spiralcraft.net.http.RangeHeader;

/**
 * Serves a contextual VFS resource
 */
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

  
  private Resource translatePath(String relativePath)
    throws UnresolvableURIException,IOException
  { 
    URI contextURI;
    try
    { contextURI=new URI("context:"+relativePath);
    }
    catch (URISyntaxException x)
    { 
      throw new UnresolvableURIException
        (relativePath,"Could not resolve this path in the context: scheme ",x);
    }
    
    if (contextURI.getAuthority()!=null)
    {
      throw new UnresolvableURIException
        (contextURI,"An authority is not permitted in this context");
    }
    
    Resource mappedResource
      =Resolver.getInstance().resolve(contextURI);
    if (mappedResource!=null && mappedResource.exists())
    { 
      if (debugLevel.canLog(Level.DEBUG))
      { log.debug("FileServlet mapped "+relativePath+" to "+mappedResource.getURI());
      }
      return mappedResource;
    }
    else
    {
      if (debugLevel.canLog(Level.DEBUG))
      { log.debug("context:"+relativePath+" did not resolve");
      }
    }
    
    
    String filePath=getServletConfig().getServletContext()
      .getRealPath(relativePath);
    
    mappedResource=Resolver.getInstance().resolve(new File(filePath).toURI());

//  How do we get a fallback resource for this relative path? Ask a NavContext?
//  or something else? Only NavContext can know virtual paths right now
//    
//    if (mappedResource.exists())
//    { 
//      return mappedResource;
//    }
//    
//    try
//    { contextURI=new URI("context://resources/"+relativePath);
//    }
//    catch (URISyntaxException x)
//    { 
//      throw new UnresolvableURIException
//        (relativePath,"Could not resolve this path in the context: scheme ",x);
//    }
//    
//    
//    mappedResource
//      =Resolver.getInstance().resolve(contextURI);
//    if (mappedResource!=null)
//    { 
//      if (debugLevel.canLog(Level.DEBUG))
//      { log.debug("FileServlet mapped "+relativePath+" to "+mappedResource.getURI());
//      }
//    }
//    else
//    {
//      if (debugLevel.canLog(Level.DEBUG))
//      { log.debug("context:"+relativePath+" did not resolve");
//      }
//    }

    return mappedResource;
    
    
    
  }
    
  @Override
  public void service(HttpServletRequest request,HttpServletResponse response)
    throws IOException,ServletException
  {
    if (debugLevel.canLog(Level.DEBUG))
    { log.debug("Servicing request for "+request.getRequestURI());
    }
    
    if (hidden(request.getServletPath()))
    {
      send404(request,response);
      return;
    }
    
    String contextPath=getContextRelativePath(request);
    
    if (contextPath==null)
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
    
    if (!contextPath.startsWith("/"))
    { 
      log.warning("Illegal path "+contextPath);
      send404(request,response);
      return;
    }
    
    
    Resource resource;
    try
    { resource=translatePath(contextPath);
    }
    catch (UnresolvableURIException x)
    { 
      log.warning("Invalid URI syntax "+contextPath);
      send400(request,response);
      return;
    }


    if (debugLevel.canLog(Level.DEBUG))
    { log.log(Level.DEBUG,"File Servlet serving "+contextPath);
    }
    
    if (request.getRequestURI().endsWith("/"))
    {
      Container container=resource.asContainer();
      if (container==null)
      { 
        send404(request,response);
        return;
      }
      else
      {
        Resource defaultResource=findDefaultFile(container);
        if (defaultResource!=null)
        { 
          if (request.getMethod().equals("GET"))
          { sendFile(request,response,defaultResource);
          }
          else if (request.getMethod().equals("HEAD"))
          { sendHead(request,response,defaultResource);
          }
          else if (request.getMethod().equals("PUT"))
          { putFile(request,response,defaultResource);
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
            { sendDirectory(request,response,container);
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
      if (resource.asContainer()!=null)
      { 
        response.sendRedirect
          (response.encodeRedirectURL(request.getRequestURI()+"/"));
      }
      else if (request.getMethod().equals("GET"))
      { sendFile(request,response,resource);
      }
      else if (request.getMethod().equals("HEAD"))
      { sendHead(request,response,resource);
      }
      else if (request.getMethod().equals("PUT"))
      { putFile(request,response,resource);
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
  private Resource findDefaultFile(Container dir)
  {
    for (int i=0;i<_defaultFiles.length;i++)
    { 
      try
      {
        Resource child=dir.getChild(_defaultFiles[i]);
        if (child.exists())
        { return child;
        }
      }
      catch (UnresolvableURIException x)
      { 
        log.log
          (Level.WARNING,"Error checking for default file "+_defaultFiles[i],x);
      }
      catch (IOException x)
      {
        log.log
          (Level.WARNING,"Error checking for default file "+_defaultFiles[i],x);
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
    ,Resource resource
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
      +"</STRONG>, could not be found on this server."
      );        
    
  }
  
  private void send400(HttpServletRequest request,HttpServletResponse response)
    throws IOException
  {
    response.sendError
    (400
    ,"<H2>400 - Bad Request</H2>The specified request, <STRONG>"
    +request.getRequestURI()
    +"</STRONG>, contains invalid syntax."
    );        
  
  }
  
  /**
   * Send the headers for the specified file to the client
   */
  private void sendHead
    (HttpServletRequest request
    ,HttpServletResponse response
    ,Resource resource
    )
    throws IOException
  {
    try
    {
      if (!resource.exists())
      { 
        response.setStatus(404);
        response.getOutputStream().flush();
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
      response.setStatus(404);
      response.getOutputStream().flush();
    }
    catch (IOException x)
    { 
      if (!x.getMessage().equals("Broken pipe")
          && !x.getMessage().equals("Connection reset by peer")
          )
      {
        log.log
          (Level.WARNING
          ,"IOException retrieving "+resource.getURI()+": "+x.toString()
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
    ,Resource resource
    )
    throws IOException
  {
    // Standard file service
    // Simply send the disk file
    
    InputStream resourceInputStream=null;
    
    try
    {
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
          ,"IOException retrieving "+resource.getURI()+": "+x.toString()
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
    ,Container container
    )
    throws IOException
  {
    if (debugLevel.isDebug())
    { log.log(Level.DEBUG,"Listing "+container.getURI());
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

    Resource[] dirs
      =container.listChildren
        (new ResourceFilter()
          {
            @Override
            public boolean accept(Resource resource)
            { return resource.asContainer()!=null;
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
        Resource subdir=dirs[i];
        if (!hidden(subdir.getURI().getPath()))
        {
          String dirname=dirs[i].getLocalName();
          
          out.append("<TR>");
          out.append("<TD align=\"left\"><TT>");
          out.append(_fileDateFormat.format(new Date(subdir.getLastModified())));
          out.append("</TT></TD>");

          out.append("<TD>");
          out.append("</TD>");

          out.append("<TD><TT>");
          out.append("<A href=\"");
          out.append(request.getScheme()+"://");
          out.append(host);
          out.append(uri);
          out.append(dirname);
          out.append("/\">");
          out.append(dirname);
          out.append("/</A>");
          out.append("</TT></TD>");
          out.append("</TR>\r\n");
        }
      }
    }

    Resource[] files
      =container.listChildren
        (new ResourceFilter()
          {
            @Override
            public boolean accept(Resource resource)
            { return resource.asContainer()==null;
            }
          }
        );

    if (files!=null)
    {
      for (int i=0;i<files.length;i++)
      { 
        Resource file=files[i];
        if (!hidden(file.getURI().getPath()))
        {
          String fileName=file.getLocalName();
          out.append("<TR>");

          out.append("<TD  align=\"left\"><TT>");
          out.append(_fileDateFormat.format(new Date(file.getLastModified())));
          out.append("</TT></TD>");

          out.append("<TD align=\"right\"><TT>");
          out.append(file.getSize());
          out.append("</TT></TD>");

          out.append("<TD><TT>");
          out.append("<A href=\"");
          out.append(request.getScheme()+"://");
          out.append(host);
          out.append(uri);
          out.append(fileName);
          out.append("\">");
          out.append(fileName);
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

}
