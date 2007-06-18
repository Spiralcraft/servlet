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

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import java.net.URI;

public class RedirectFilter
    extends AutoFilter
{

  private String redirectURL;

  public void setRedirectURL(String url)
  { 
    URI.create(url);
    this.redirectURL=url;
    
  }
  
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException
  {
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    httpResponse.sendRedirect(httpResponse.encodeRedirectURL(redirectURL));
  }

  public String getFilterType()
  { return "redirect";
  }

  public void setParentInstance(AutoFilter parentInstance)
  {

  }

}
