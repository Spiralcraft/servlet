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
package spiralcraft.servlet.gzip;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import spiralcraft.servlet.autofilter.AutoFilter;


public class GZipFilter
    extends AutoFilter
{

  private HashSet<String> contentTypes=new HashSet<>();
  {
    contentTypes.add("application/javascript");
    contentTypes.add("application/json");
    contentTypes.add("application/x-javascript");
    contentTypes.add("application/xml");
    contentTypes.add("application/xml+rss");
    contentTypes.add("application/xhtml+xml");
    contentTypes.add("application/x-font-ttf");
    contentTypes.add("application/x-font-opentype");
    contentTypes.add("application/vnd.ms-fontobject");
    contentTypes.add("image/svg+xml");
    contentTypes.add("image/x-icon");
    contentTypes.add("application/rss+xml");
    contentTypes.add("application/atom_xml");
  }

  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException,ServletException
  {
    // Do the compression
    HttpServletResponseCompressionWrapper compressResponse
      =new HttpServletResponseCompressionWrapper
        ((HttpServletRequest) request,(HttpServletResponse) response,this);
    try
    {
      chain.doFilter(request,compressResponse);
    }
    finally
    { compressResponse.finish();
    }
    
    
  }
  
  boolean shouldCompress
    (HttpServletRequest  httpRequest
    ,HttpServletResponseCompressionWrapper response
    )
  { 
    String acceptEncoding=httpRequest.getHeader("Accept-Encoding");
    String userAgent=httpRequest.getHeader("User-Agent");
    String contentType=response.getContentType();
    
    if (acceptEncoding!=null 
        && acceptEncoding.indexOf("gzip")>=0
        && contentType!=null
        && (contentType.startsWith("text/")
            || contentTypes.contains(majorContentType(contentType))
           )
            
       )
    { 
      if (debug)
      { log.fine("Positive gzip match: acceptEncoding="+acceptEncoding);
      }
      String ieVersion=null;
      if (userAgent!=null)
      { 
        int index=userAgent.indexOf("MSIE ");
        if (index>-1)
        { ieVersion=userAgent.substring(index+5,index+6);
        }
      }
      
      if (ieVersion!=null 
          && "56".contains(ieVersion)
         )
      {
        // XXX Figure out how to be more selective about MSIE 6 conditions,
        //   but there are a multitude of associated gzip bugs
        if (debug)
        { log.fine("Skipping gzip for MSIE "+ieVersion);
        }
        return false;
      }
      else
      { return true;
      }
    }
    return false;
  }
  
  private String majorContentType(String contentType)
  {
    int semicolon=contentType.indexOf(';');
    return semicolon<0
        ?contentType
        :contentType.substring(0,semicolon);
    
  }
}