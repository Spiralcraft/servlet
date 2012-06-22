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
import java.util.Stack;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.common.ContextualException;
import spiralcraft.common.LifecycleException;
import spiralcraft.data.persist.XmlBean;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

/**
 * <p>Provides a context for functionality within the subtree addressed by a 
 *   given URI path.
 * </p>
 * 
 * <p>The Controller filter loads a PathContextFilter for each element of
 *   the content tree. A PathContextFilter may extend the content
 *   tree and indirectly invoke other PathContextFilters.
 * </p>
 * 
 * @author mike
 *
 */
public class PathContextFilter
  extends FocusFilter<Object>
{

  @SuppressWarnings("unchecked")
  public static final PathContext get(HttpServletRequest request)
  { 
    Stack<PathContext> stack
      =(Stack<PathContext>) 
        request.getAttribute("spiralcraft.servlet.PathContext");
    return stack!=null?stack.peek():null;
  }

  
  @SuppressWarnings("unchecked")
  protected static final void push
    (HttpServletRequest request
    ,PathContext context
    )
  {
    Stack<PathContext> stack
      =(Stack<PathContext>) 
        request.getAttribute("spiralcraft.servlet.PathContext");
    if (stack==null)
    {
      stack=new Stack<PathContext>();
      request.setAttribute("spiralcraft.servlet.PathContext",stack);
    }
    stack.push(context);
    
  }
  
  @SuppressWarnings("unchecked")
  protected static final void pop(HttpServletRequest request)
  { 
    Stack<PathContext> stack
      =(Stack<PathContext>) 
        request.getAttribute("spiralcraft.servlet.PathContext");
    if (stack!=null)
    { stack.pop();
    }
    
  }
  
  { setUsesRequest(true);
  }
  
  private URI codeSearchRoot;
  private PathContext context;
  private URI resourceURI;
  private AutoFilter filterSet;
  

  
  @SuppressWarnings("unchecked")
  @Override
  protected Focus<Object> createFocus(
    Focus<?> parentFocus)
    throws ContextualException
  {
    if (debug)
    { log.fine("Binding PathContextFilter for "+getPath());
    }

    Focus<Object> chain=(Focus<Object>) parentFocus;
    PathContext parentContext
       =LangUtil.findInstance(PathContext.class,chain);
    try
    {
      // Find the PathContext object.
      // 
      // 
      codeSearchRoot=
        URIUtil.ensureTrailingSlash
          (URI.create("context://code"+getPath().format('/'))
          );
      resourceURI=codeSearchRoot.resolve("PathContext.assy.xml");
      Resource resource
        =Resolver.getInstance().resolve(resourceURI);
      
      if (!resource.exists())
      {
        if (parentContext==null)
        { 
          String defaultContainer
            =this.config.getServletContext()
              .getInitParameter("spiralcraft.servlet.app.model");
          if (defaultContainer!=null)
          {
            codeSearchRoot
              =URIUtil.ensureTrailingSlash(URI.create(defaultContainer));
            resourceURI
              =codeSearchRoot.resolve("PathContext.assy.xml");
            resource
              =Resolver.getInstance().resolve(resourceURI);
            if (resource.exists())
            { 
              context=XmlBean.<PathContext>instantiate
               (codeSearchRoot.resolve("PathContext")).get();
            }
            else
            { context=new PathContext();
            }
          }
        }
        else
        { 
          if (debug)
          {
            log.fine
              ("Parent context path for "
                +getPath()+" is "+parentContext.getAbsolutePath()
              );
          }
          codeSearchRoot
            =parentContext.mapRelativePath
              (parentContext.getAbsolutePath().relativize(getPath())
                .format('/'));
          if (codeSearchRoot!=null)
          {
            resourceURI
              =codeSearchRoot.resolve("PathContext.assy.xml");
            resource
              =Resolver.getInstance().resolve(resourceURI);
            if (resource.exists())
            { 
              context=XmlBean.<PathContext>instantiate
               (codeSearchRoot.resolve("PathContext")).get();
            }
            else
            { context=new PathContext();
            }
          }
        }
      }
      else
      {
        context=XmlBean.<PathContext>instantiate
            (codeSearchRoot.resolve("PathContext"))
              .get();
      }

      if (context!=null)
      { 
        context.setAbsolutePath(getPath());
        context.setContentResource(getContainer().asContainer());
        context.setDefaultCodeBaseURI(codeSearchRoot);
        context.setParent(parentContext);
        
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
    { 
      context.push();
      push(request,context);
    }
  }
  

  @Override
  protected void popSubject(
    HttpServletRequest request)
  { 
    if (context!=null)
    { 
      pop(request);
      context.pop();
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
      .append("{\r\nURI Path: ").append(getPath().format('/'))
      .append("\r\nContent loc: ").append(getContainer().getURI())
      .append("\r\nCode search root: ").append(codeSearchRoot)
      .append("\r\nPathContext: ").append(context)
      .append("}");
      ;
    return buf.toString();
  }
}