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
package spiralcraft.servlet.pages;

import spiralcraft.servlet.HttpServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;
import java.io.Writer;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

import java.util.HashMap;

import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglCompiler;
import spiralcraft.textgen.compiler.DocletUnit;
import spiralcraft.textgen.Element;


import spiralcraft.text.ParseException;

import spiralcraft.servlet.HttpFocus;

import spiralcraft.servlet.autofilter.spi.FocusFilter;

import spiralcraft.lang.Focus;


/**
 * <P>An HttpServlet which serves pages generated by the spiralcraft.textgen 
 *   generation engine.
 * </P>
 *   
 * <P>There is one GeneratorServlet per servlet context. There is one copy of
 *   each bound (compiled) page in the context.
 * </P>
 * 
 * <P>This servlet is usually mapped to the .ghtml ("generated-html") extension
 *   and provides a limited amount of dynamic functionality in a lightweight,
 *   highly efficient manner.
 * </P>
 * 
 * @author mike
 *
 */
public class GeneratorServlet
  extends HttpServlet
{
  
  private final HashMap<String,ResourceEntry> resourceMap
    =new HashMap<String,ResourceEntry>();
  
  private HttpFocus<?> httpFocus;
  

  @Override
  public void service(ServletRequest srequest,ServletResponse sresponse)
    throws ServletException,IOException
  {
    HttpServletRequest request
      =(HttpServletRequest) srequest;
    
    HttpServletResponse response
      =(HttpServletResponse) sresponse;

    if (httpFocus==null)
    {
      // Initialize the local HTTP Focus with its parent that's always passed
      //   via the request
      
      httpFocus
        =new HttpFocus<Void>(FocusFilter.getFocusChain(request));
    }
    else
    {
      Focus<?> parentFocus=FocusFilter.getFocusChain(request);
      if (parentFocus!=null && httpFocus.getParentFocus()==null)
      { log.fine("HTTPFocus late parent binding to "+parentFocus);
      }
    }
    
    try
    {
      httpFocus.push
        (this.getServletConfig().getServletContext()
        ,request
        ,response
        );
      
      ResourceEntry entry=getEntry(request);
      if (entry==null)
      { 
        response.sendError(404,request.getRequestURI()+" not found");
        return;
      }
      entry.service(response);
    }
    catch (URISyntaxException x)
    { throw new ServletException(x.toString(),x);
    }
    catch (MalformedURLException x)
    { throw new ServletException(x.toString(),x);
    }
    catch (UnresolvableURIException x)
    { throw new ServletException(x.toString(),x);
    }
    finally
    { httpFocus.pop();
    }
    
  }
  
  /**
   * Obtain a ResourceEntry for the specified request. ResourceEntries are
   *   cached for resources that exist.
   * 
   * @param request
   * @return
   */
  private ResourceEntry getEntry(HttpServletRequest request)
    throws URISyntaxException
      ,MalformedURLException
      ,UnresolvableURIException
      ,IOException
  {
    String resourcePath=request.getPathInfo();
    if (resourcePath==null || resourcePath.length()==0)
    { resourcePath=request.getServletPath();
    }
    
    if (resourcePath==null)
    { return null;
    }
    
    // System.err.println("GeneratorServlet: resource path="+resourcePath);
    ResourceEntry entry;
    synchronized (resourceMap)
    {
      entry=resourceMap.get(resourcePath);
      if (entry==null)
      { 
        URI uri
          =getServletConfig()
            .getServletContext()
              .getResource(resourcePath)
                .toURI();
        Resource resource=Resolver.getInstance().resolve(uri);
        if (resource.exists())
        { 
          entry=new ResourceEntry(resource,httpFocus);
          resourceMap.put(resourcePath,entry);
        }
        
      }
      else
      {
        if (!entry.getResource().exists())
        { 
          resourceMap.remove(resourcePath);
          entry=null;
        }
      }
    }
    
    return entry;
  }
  
 
}

class ResourceEntry
{
  private final Resource resource;
  private final Focus<?> focus;
  private long lastRead;
  private DocletUnit unit;

  private Element element;
  private Exception exception;
  
  public ResourceEntry(Resource resource,HttpFocus<?> focus)
  { 
    this.resource=resource;
    this.focus=focus;
  }
  
  public Resource getResource()
  { return resource;
  }
  
  public synchronized void checkState()
    throws IOException
  {
    long lastModified;
    if (unit!=null)
    { 
      lastModified=unit.getLastModified();
//      System.err.println("GeneratorServler: lastModifier="+lastModified);
    }
    else
    { lastModified=resource.getLastModified();
    }
    
    if (lastModified>lastRead)
    { recompile();
    }
    lastRead=lastModified;     
  }
  
  private void recompile()
  {
    try
    { 
      unit=new TglCompiler<DocletUnit>().compile(resource.getURI());
      element=unit.bind(focus);
      exception=null;
    }
    catch (IOException x)
    { 
      element=null;
      exception=x;
    }
    catch (ParseException x)
    { 
      element=null;
      exception=x;
    }
  }
  
  
  public void service
    (HttpServletResponse response)
    throws IOException
  {
    checkState();
    if (exception!=null)
    { 
      response.sendError(501,exception.toString());
      // XXX Figure out where to log this stuff
      exception.printStackTrace();
    }
    else
    { 
      Writer writer=response.getWriter();
      EventContext context=new EventContext(writer,false,null);
      element.render(context);
      writer.flush();
    }
  }
  
}
