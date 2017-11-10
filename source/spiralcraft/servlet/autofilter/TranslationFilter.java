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
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.log.Level;
import spiralcraft.net.http.Headers;
import spiralcraft.net.http.RangeHeader;
import spiralcraft.time.Clock;
import spiralcraft.util.Path;

import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.Translator;

/**
 * <P>A filter which generates a virtual translated resources from existing
 *   resources- ie. request for URLs that do not directly map to a resource,
 *   but can be generated on-the-fly from a closely related resource with a
 *   similar URL.
 * 
 * <P>This functions by rewriting the filename according to specific criteria
 *   in order to find the original resource, and applying a translator to the
 *   original resource to create the response.
 *   
 * <P>Examples include:
 * <UL>
 * 
 *   <LI>Returning a compressed version of [resource] when [resource].zip is
 *     requested
 *   </LI>
 *   
 *   <LI>Returning a [resource].m3u automatically generated from [resource].mp3
 *     to facilitate fast media player launching
 *   </LI>
 *   
 *   <LI>Returning a layout of [directory] when [directory].html is requested.
 *   </LI>
 *   
 *   
 * </UL> 
 */
public class TranslationFilter
    extends AutoFilter
{

  private String originalSuffix;
  private String translatedSuffix;
  private String contentType;
  private Translator translator;
  private int defaultCacheSeconds;
  private int bufferSize=8192;
  
  private final HashMap<URI,TranslationBuffer> translationMap
    =new HashMap<>();
  
  /**
   * <P>The suffix of the resource that will be translated. If
   *   specified, the suffix of the requested file will be replaced with the
   *   original suffix to find the original file.
   * 
   * <P>For example, if set to "mp3", a request for [file].m3u will create a
   *   response based on [file].mp3
   *   
   * <P>If this is not specified, the requested (derivative) filename will
   *   have its suffix removed to create the original filename.
   */
  public void setOriginalSuffix(String suffix)
  { this.originalSuffix=suffix;
  }
  
  /**
   * <P>The last part of the translated filename that will be changed to the
   *   originalSuffix when the filename is rewritten to find the original
   *   file.
   * 
   * <P>If not specified, and the pattern for this filter indicates a suffix
   *   match (ie. *.xyz), the value of this option will be the suffix specified
   *   in the pattern.
   * 
   * <P>For example, if the translatedSuffix is set to .m3u, and the original
   *   suffix is set to .mp3, a request for [file].m3u will result in an
   *   original filename of [file].mp3.
   */
  public void setTranslatedSuffix(String suffix)
  { this.translatedSuffix=suffix;
  }
  
  /**
   * The Translator that will do the translating
   */
  public void setTranslator(Translator translator)
  { this.translator=translator;
  }

  /**
   * @param contentType
   */
  public void setContentType(String contentType)
  { this.contentType=contentType;
  }
  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    ,FilterChain chain
    ) throws IOException,ServletException
  {
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    
    String requestURL=((HttpServletRequest) request).getRequestURL().toString();
    if (debug)
    { log.fine("TranslatorFilter: "+requestURL);
    }
    
    PathContext pathContext=PathContext.instance(); 
    if (debug)
    {
      log.fine("PathContext "+pathContext);
      log.fine("PathInfo "+pathContext.getPathInfo());
      log.fine("CodeBase "+pathContext.getEffectiveCodeBaseURI());
    }
    
    Resource targetResource=pathContext.resolveCode(pathContext.getPathInfo());
    if (debug)
    { log.fine("Target="+targetResource);
    }
    
    if (targetResource!=null && targetResource.exists())
    { 
      // Return the resource directly
      chain.doFilter(request,response);
    }
    else
    { 
      Resource translation=findTranslation
        (URI.create(requestURL)
        ,pathContext.getEffectiveCodeBaseURI().resolve(pathContext.getPathInfo())
        );
      if (translation!=null)
      { 
        if (httpRequest.getMethod().equals("GET"))
        { sendFile(httpRequest,httpResponse,translation);
        }
        else if (httpRequest.getMethod().equals("HEAD"))
        { sendHead(httpRequest,httpResponse,translation);
        }      
        
      }
      else
      { 
        // Default to the standard response
        chain.doFilter(request,response);
      }
    }
    
  }

  private synchronized Resource findTranslation(URI requestURI, URI targetURI)
    throws IOException
  {
    TranslationBuffer translation=translationMap.get(targetURI);
    if (translation==null)
    { 
      translation=createTranslation(requestURI,targetURI);
      if (translation!=null)
      { translationMap.put(targetURI,translation);
      }
    }
    else
    {
      if (translation.sourceModified<translation.source.getLastModified())
      { 
        translation.sourceModified=translation.source.getLastModified();
        translation.translation=translator.translate(translation.source,requestURI);
        translation.translation.setLastModified(translation.sourceModified);
      }
    }
    return translation!=null?translation.translation:null;
  }
  
  private TranslationBuffer createTranslation(URI requestURI,URI targetURI)
    throws IOException
  {
    if (translatedSuffix==null
        && pattern.startsWith("*")
        && pattern.length()>2
        )
    { translatedSuffix=pattern.substring(2);
    }
    
    Path targetPath=new Path(targetURI.getPath(),'/');
    String targetFilename=targetPath.lastElement();
    String sourceFilename;
    if (originalSuffix!=null)
    {
      if (targetFilename.endsWith(translatedSuffix))
      { 
        sourceFilename
          =targetFilename
            .substring(0,targetFilename.length()-(translatedSuffix.length()+1))
            .concat(".")
            .concat(originalSuffix);
      }
      else
      { return null;
      }
    }
    else
    {
      sourceFilename
        =targetFilename
          .substring(0,targetFilename.length()-(translatedSuffix.length()+1));
    }
    
    Path sourcePath=targetPath.parentPath().append(sourceFilename);
    
    try
    {
      URI sourceURI
        =targetURI.resolve(new URI(null,null,sourcePath.format("/"),null));
    
      Resource sourceResource=Resolver.getInstance().resolve(sourceURI);
      if (sourceResource.exists())
      { 
        TranslationBuffer translation=new TranslationBuffer();
        translation.source=sourceResource;
        translation.sourceModified=sourceResource.getLastModified();
        translation.translation=translator.translate(sourceResource,requestURI);
        translation.translation.setLastModified(translation.sourceModified);
        translation.uri=targetURI;
        return translation;
      }
      else
      { return null;
      }
    }
    catch (URISyntaxException x)
    { 
      x.printStackTrace();
      return null;
    }
    
  }


  private void setHeaders
    (HttpServletResponse response
    ,Resource resource
    )
    throws IOException
  {
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
  
  private long floorToSecond(long timeInMs)
  { return (long) Math.floor((double) timeInMs/(double) 1000)*1000;
  }
  
  private void sendHead
    (HttpServletRequest request
    ,HttpServletResponse response
    ,Resource resource
    )
    throws IOException
  {
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
        else if (ifModifiedSince>0 && log.canLog(Level.DEBUG))
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
        else if (ifModifiedSince>0 && log.canLog(Level.DEBUG))
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
        ,bufferSize
        ,contentLength
        );
      
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
    finally
    { 
      if (resourceInputStream!=null)
      { resourceInputStream.close();
      }
    }
  }
  
}

class TranslationBuffer
{
  URI uri;
  Resource translation;
  Resource source;
  long sourceModified;
}
