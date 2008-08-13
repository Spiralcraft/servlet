//
// Copyright (c) 1998,2005 Michael Toth
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
package spiralcraft.servlet.util;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import spiralcraft.servlet.HttpServlet;


/**
 * <p>A Servlet which simply logs interface methods in order to facilitate
 *   or debug integration with a container.
 * </p>
 * 
 *  
 * @author mike
 *
 */
public class StubServlet
  extends HttpServlet
{
 
  @Override
  public void init(ServletConfig config)
    throws ServletException
  { 
    log.fine("init: "+config.toString());
    super.init(config);
  }

  @Override
  public void destroy()
  { 

    log.fine("destroy");
    super.destroy();
  }

  @Override
  public void service(ServletRequest request, ServletResponse response)
      throws ServletException, IOException
  { 
    log.fine("service: "+request);
  }
  
}
