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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

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
  
  private final HashMap<URI,Resource> translationMap
    =new HashMap<URI,Resource>();
  
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
    String requestURI=((HttpServletRequest) request).getRequestURI().toString();
    String requestURL=((HttpServletRequest) request).getRequestURL().toString();
    System.err.println("TranslatorFilter: "+requestURL);
    File targetFile
      =new File
        (config.getServletContext().getRealPath
          ( requestURI
         )
        );
    
    if (targetFile.exists())
    { 
      // Return the resource directly
      chain.doFilter(request,response);
    }
    else
    { 
      Resource translation=findTranslation
        (URI.create(requestURL)
        ,targetFile.toURI()
        );
      if (translation!=null)
      { 
        if (contentType!=null)
        { response.setContentType(contentType);
        }
        
        // Send the translated response
        
        InputStream in = translation.getInputStream();
        try
        {
          OutputStream out = response.getOutputStream();
        
          StreamUtil.copyRaw(in,out,8192);

          out.flush();

        }
        finally
        { in.close();
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
    Resource translation=translationMap.get(targetURI);
    if (translation==null)
    { 
      translation=createTranslation(requestURI,targetURI);
      if (translation!=null)
      { translationMap.put(targetURI,translation);
      }
    }
    return translation;
  }
  
  private Resource createTranslation(URI requestURI,URI targetURI)
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
      { return translator.translate(sourceResource,requestURI);
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



}
