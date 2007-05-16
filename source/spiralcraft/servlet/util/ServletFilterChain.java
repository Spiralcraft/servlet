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
package spiralcraft.servlet.util;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.Servlet;

/**
 * Implements the end of a Filter chain by invoking a Servlet
 */
public class ServletFilterChain
  implements FilterChain
{
  private Servlet servlet;
  
  public void setServlet(Servlet servlet)
  { this.servlet=servlet;
  }
  
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException,IOException
  { servlet.service(request,response);
  }

}
