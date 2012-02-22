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
package spiralcraft.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import spiralcraft.data.persist.AbstractXmlObject;
import spiralcraft.lang.BindException;
import spiralcraft.servlet.kit.HttpServlet;


/**
 * <p>A Servlet which delegates to another Servlet loaded from
 *   a configuration file via the spiralcraft.data.persist mechanism.
 * </p>
 * 
 * <p>If the initialization parameter named 'resource' is supplied, a
 *   spiralcraft.data xml file or other data resource referenced by 
 *   the specified URI contains the servlet definition.
 * </p>
 *  
 * <p>If the initialization parameter named 'type' is supplied, the
 *   type resolved from the specified URI (e.g. an AssemblyClass or a Java
 *   class) is instantiated to provide the servlet.
 * </p>

 * @author mike
 *
 */
public class BeanServlet
  extends HttpServlet
{
  
  private URI resourceURI;
  private AbstractXmlObject<Servlet,?> ref;
  private Servlet delegate;
  
  { 
    recognizedParameters=new HashSet<String>();
    recognizedParameters.add("resource");
    recognizedParameters.add("typeURI");
  }
  
  @Override
  protected void setInitParameter(String name,String value)
    throws ServletException
  { 
    if (name.equals("resource"))
    { 
      
      resourceURI=URI.create(value);
      if (!resourceURI.isAbsolute())
      { resourceURI=getResource(resourceURI.toString()).getURI();
      }
      
      try
      {
        ref=AbstractXmlObject.<Servlet>create
          (URI.create("class:/javax/servlet/Servlet")
          , resourceURI
          );
        delegate=ref.get();
        delegate.init(getServletConfig());
      }
      catch (BindException x)
      { throwServletException("Error loading servlet bean", x);
      }
    }
    
    if (name.equals("typeURI"))
    {
      try
      {
        ref=AbstractXmlObject.<Servlet>instantiate(URI.create(value));
        delegate=ref.get();
        delegate.init(getServletConfig());
      }
      catch (BindException x)
      { throwServletException("Error loading servlet bean", x);
      }
    }
    
  }

  @Override
  public void destroy()
  { 
    ref.get().destroy();
    super.destroy();
  }


  @Override
  public String getServletInfo()
  { 
    if (delegate!=null)
    { return super.getServletInfo()+": " +delegate.getServletInfo();
    }
    else
    { return super.getServletInfo();
    }
  }

  @Override
  public void service(ServletRequest request, ServletResponse response)
      throws ServletException, IOException
  { 
    if (delegate!=null)
    { delegate.service(request,response);
    }
    else
    { throwServletException("Servlet has not been initialized",null);
    }
  }
  
}
