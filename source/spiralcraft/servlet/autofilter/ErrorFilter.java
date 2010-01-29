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
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class ErrorFilter
    extends AutoFilter
{

  private Throwable exception;
  {
    setGlobal(true);
    setPattern("*");
  }
  
  public void setThrowable(Throwable exception)
  { this.exception=exception;
  }
  
  @Override
  public void doFilter
    (ServletRequest request
    , ServletResponse response
    , FilterChain chain
    )
    throws IOException
  {
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    httpResponse.setStatus(501);
    
    PrintWriter printWriter=new PrintWriter(response.getWriter());
    printWriter.write(exception.toString());

    exception.printStackTrace(printWriter);
    printWriter.flush();
  }

  public String getFilterType()
  { return "error";
  }

}