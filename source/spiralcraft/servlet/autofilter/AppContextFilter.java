//
//Copyright (c) 2012 Michael Toth
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
import java.net.URI;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.common.ContextualException;
import spiralcraft.common.LifecycleException;
import spiralcraft.data.persist.XmlBean;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

/**
 * <p>Provides a context for functionality used by all parts of the application
 * </p>
 * 
 * <p>The Controller filter loads the AppContextFilter before loading
 *   any PathContextFilters. 
 * </p>
 * 
 * @author mike
 *
 */
public class AppContextFilter
  extends FocusFilter<Object>
{
  
  { setUsesRequest(true);
  }
  
  private URI codeSearchRoot;
  private AppContext context;
  private URI resourceURI;
  private AutoFilter filterSet;
  
  { debug=true;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected Focus<Object> createFocus(
    Focus<?> parentFocus)
    throws ContextualException
  {
    if (debug)
    { log.fine("Binding AppContextFilter");
    }

    Focus<Object> chain=(Focus<Object>) parentFocus;
    try
    {
      // Find the AppContext object.
      // 
      // 
      codeSearchRoot=URI.create("context://code/");
      resourceURI=codeSearchRoot.resolve("AppContext.assy.xml");
      Resource resource
        =Resolver.getInstance().resolve(resourceURI);
      
      if (!resource.exists())
      {
        String defaultContainer
          =this.config.getServletContext()
            .getInitParameter("spiralcraft.servlet.app.model");
        if (defaultContainer!=null)
        {
          codeSearchRoot
            =URIUtil.ensureTrailingSlash(URI.create(defaultContainer));
          resourceURI
            =codeSearchRoot.resolve("AppContext.assy.xml");
          resource
            =Resolver.getInstance().resolve(resourceURI);
          if (resource.exists())
          { 
            context=XmlBean.<AppContext>instantiate
             (codeSearchRoot.resolve("AppContext")).get();
          }
          else
          { context=new AppContext();
          }
        }
      }
      else
      {
        context=XmlBean.<AppContext>instantiate
            (codeSearchRoot.resolve("AppContext"))
              .get();
      }

      if (context!=null)
      { 
        context.setContentResource(getContainer().asContainer());
        context.setDefaultCodeBaseURI(codeSearchRoot);
        
        chain=(Focus<Object>) context.bind(parentFocus);
        
        AutoFilter[] filters=context.getFilters();
        if (filters!=null && filters.length>0)
        { 
          filterSet=new CompoundFilter(filters);
        
          try
          { 
            filterSet.setPath(getPath());
            filterSet.setPattern(getPattern());
            filterSet.setGlobal(isGlobal());
            filterSet.setContainer(getContainer());
            filterSet.init(config);
          }
          catch (ServletException x)
          {
            throw new ContextualException
              ("Error initializing filter set for "+resourceURI,x);
          }        
        }
        

      }
    }
    catch (UnresolvableURIException x)
    { 
      throw new ContextualException
        ("Unable to resolve code root for path "+getPath().format('/')+": tried "+codeSearchRoot,x);
    }
    catch (IOException x)
    {
      throw new ContextualException
        ("Error accessing "+resourceURI,x);
    }
    
      
    if (context!=null)
    { 
      try
      { context.start();
      }
      catch (LifecycleException x)
      { 
        throw new ContextualException
          ("Unable to start context for path "+getPath().format('/')
          ,resourceURI
          ,x
          );
      }
    }
    if (debug)
    { log.config(toString());
    }
    
    return chain;
  }

  @Override
  protected void pushSubject(
    HttpServletRequest request,
    HttpServletResponse response)
    throws ContextualException,
    ServletException
  { 
    if (context!=null)
    { context.push();
    }
  }
  

  @Override
  protected void popSubject(
    HttpServletRequest request)
  { 
    if (context!=null)
    { context.pop();
    }
  }

  
  @Override
  protected void doChain
    (FilterChain chain
    ,HttpServletRequest request
    ,HttpServletResponse response
    ) throws IOException, ServletException
  {
    if (filterSet!=null)
    { filterSet.doFilter(request,response,chain);
    }
    else
    { chain.doFilter(request,response);
    }
  }
    
  @Override
  public String toString()
  {
    StringBuffer buf=new StringBuffer();
    buf.append(super.toString())
      .append("{\r\nURI Path: ").append(getPath()!=null?getPath().format('/'):"")
      .append("\r\nContent loc: ").append(getContainer()!=null?getContainer().getURI():"")
      .append("\r\nCode search root: ").append(codeSearchRoot)
      .append("\r\nPathContext: ").append(context)
      .append("}");
      ;
    return buf.toString();
  }
  

  void setContainer(Container container)
  { this.container=container;
  }
}