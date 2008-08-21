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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import spiralcraft.servlet.autofilter.AutoFilter;
import spiralcraft.servlet.autofilter.RedirectFilter;


public class GZipFilter
    extends AutoFilter
{

  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException,ServletException
  {

    HttpServletRequest httpRequest=(HttpServletRequest) request;
    

    String acceptEncoding=httpRequest.getHeader("Accept-Encoding");
    String userAgent=httpRequest.getHeader("User-Agent");
    
    
    if (acceptEncoding!=null 
        && acceptEncoding.indexOf("gzip")>=0
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
        log.fine("Skipping gzip for MSIE "+ieVersion);
        
        chain.doFilter(request,response);
      }
      else
      {
        // Do the compression
        HttpServletResponseCompressionWrapper compressResponse
          =new HttpServletResponseCompressionWrapper
            ((HttpServletResponse) response);
        compressResponse.startCompression();
        chain.doFilter(request,compressResponse);
        compressResponse.finish();
      }
    }
    else
    { chain.doFilter(request,response);
    }
    
    
  }


  @Override
  public void setGeneralInstance(AutoFilter parentInstance)
  {

  }

  @Override
  public Class<? extends AutoFilter> getCommonType()
  { return RedirectFilter.class;
  }
}