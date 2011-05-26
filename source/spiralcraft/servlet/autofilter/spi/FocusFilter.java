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
package spiralcraft.servlet.autofilter.spi;

import java.io.IOException;
import java.net.URI;

import spiralcraft.common.ContextualException;
import spiralcraft.common.namespace.ContextualName;
import spiralcraft.common.namespace.UnresolvedPrefixException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.util.ExpressionRenderer;
import spiralcraft.servlet.HttpFocus;
import spiralcraft.servlet.autofilter.AutoFilter;
import spiralcraft.text.Renderer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Exposes arbitrary context via a spiralcraft.lang Focus to servlet API
 *   request processing components (eg. Filters, Servlets).
 * </p>
 *    
 * <p>The Focus is passed via a ServletRequest attribute. It has the
 *    same lifecycle as the FocusFilter itself, and is permanently referenced
 *    by the FocusFilter instance.
 * </p>
 * 
 * <p>Provides for conditional execution via the "whenX" property.</p>
 * 
 * <p>Optionally renders output via a Renderer set with the "renderer"
 *   property instead of running the rest of the Focus chain, controlled
 *   via the renderWhenX property.
 * </p>
 * 
 * <p>Ensures that HttpServletRequest, HttpServletResponse and ServletContext
 *   are published in the Focus chain when required via the "usesRequest"
 *   property.
 * </p>
 *
 * <p>Permits differentiation between multiple Filters that publish the same
 *   type via the "alias" property.
 * </p>
 */
public abstract class FocusFilter<T>
  extends AutoFilter
{
  
  private static final String attributeName="spiralcraft.lang.focus";

  /**
   * <p>Obtain the Focus associated with the deepest FocusFilter in the
   *   stack.
   * </p>
   * 
   * <p>XXX If we are building context duration, this should always return the
   *   same Focus, because the Filter object is also context duration. Perhaps
   *   the filter should just get the Focus from its parent.
   *   
   *   2008-08-06 This really needs to be done, along with integrating Focus
   *     into the "webapp" model.
   *   2010-02-03 Putting the Focus in ThreadLocal may be the most
   *     portable solution
   * </p>
   * 
   * @param request 
   * @return The Focus
   */
  public static Focus<?> getFocusChain(HttpServletRequest request)
  { return (Focus<?>) request.getAttribute(attributeName);
  }

  public static void setFocusChain(HttpServletRequest request,Focus<?> focus)
  { request.setAttribute(attributeName,focus);
  }
  
  private HttpFocus<?> httpFocus;
  private Binding<Boolean> whenX;
  
  private Binding<Boolean> renderWhenX;
  private Binding<Integer> responseCodeX;
  private Binding<String> contentTypeX;
  
  private boolean usesRequest;
  private Focus<T> focus;
  private Focus<?> exportFocus;
  private URI alias;
  private Renderer renderer;
  private Expression<?> outputX;
  private volatile boolean initialized;
  
  
  // Default to global, to implement Focus hierarchy
  { setGlobal(true);
  }
  
  { super.setPattern("*");
  }
  
  @Override
  public void setPattern(String pattern)
  { 
    if (!pattern.equals("*"))
    {
      throw new IllegalArgumentException
        ("Cannot change Filter pattern: Filter class "+getClass().getName()
        +" must always be in the request chain. Use whenX property to control"
        +" when filter is active"
        );
    }
  }
  
  /**
   * A Boolean Expression which controls when this filter will be run.
   * 
   * @param whenX
   */
  public void setWhenX(Binding<Boolean> whenX)
  { this.whenX=whenX;
  }
  
  /**
   * A Boolean Expression which controls when the Renderer is used to
   *   generate the response instead of executing the rest of the filter chain.
   * 
   * @param renderWhenX
   */
  public void setRenderWhenX(Binding<Boolean> renderWhenX)
  { this.renderWhenX=renderWhenX;
  }
  
  /**
   * <p>An optional Renderer to render  output from this Filter to
   *   the HTTP client in lieu of executing the rest of the Filter chain
   * </p>
   * 
   * <p>If a renderer is present, the rest of the filter chain will not
   *   be executed.
   * </p>
   * 
   * @param renderer
   */
  public void setRenderer(Renderer renderer)
  { this.renderer=renderer;
  }  
  
  /**
   * <p>An Expression to output. This will provide the context for the 
   *   specified Renderer or will cause a Renderer to be created if none
   *   was specified.
   * </p>
   * 
   * @param outputX
   */
  public void setOutputX(Expression<?> outputX)
  { this.outputX=outputX;
  }
  
  /**
   * An Expression for the response code associated with the renderer
   * 
   * @param responseCodeX
   */
  public void setResponseCodeX(Binding<Integer> responseCodeX)
  { this.responseCodeX=responseCodeX;
  }

  /**
   * An Expression for the content type associated with the renderer, which
   *   will be used to set the Content-Type response header.
   * 
   * @param responseCodeX
   */
  public void setContentTypeX(Binding<String> contentTypeX)
  { this.contentTypeX=contentTypeX;
  }

  public Focus<T> getFocus()
  { return focus;
  }

  /**
   * <p>Assign an alias to the Focus to be published to disambiguate between
   *   multiple Foci of the same type.
   * </p>
   * 
   * @param alias
   */
  public void setAlias(ContextualName alias)
    throws UnresolvedPrefixException
  { this.alias=alias.getQName().toURIPath();
  }
  
  /**
   * <p>Indicate that this FocusFilter contains Expressions which bind to
   *   the HttpServletRequest or the ServletContext, and that these items
   *   should be made available on every request.
   * </p>
   * 
   * @param usesRequest
   */
  public void setUsesRequest(boolean usesRequest)
  { this.usesRequest=usesRequest;
  }
  
  /**
   * Create a new instance of the Focus that will be inserted into the Focus
   *   chain (scoped to the lifetime of the FocusFilter). This method will
   *   be called only once.
   */
  protected abstract Focus<T> createFocus
    (Focus<?> parentFocus)
    throws ContextualException;
  
  /**
   * Populate the subject of the Focus with the appropriate object for
   *   the Thread processing this request.
   *   
   * @param request
   */
  protected abstract void pushSubject
    (HttpServletRequest request,HttpServletResponse response)
    throws ContextualException,ServletException;
  
  /**
   * Restore the subject of the Focus with the object it referenced for this
   *   thread before the pushSubject() method was called.
   *   
   * @param request
   */
  protected abstract void popSubject(HttpServletRequest request);
  
  /**
   * Override to prepare the local context and resolve any imported
   *   dependencies before createFocus() is called to publish the managed
   *   object.
   * 
   * @param parentFocus
   * @return
   */
  protected Focus<?> bindImports(Focus<?> parentFocus)
    throws BindException
  { return parentFocus;
  }
  
  /**
   * Override to export additional data into the Focus chain that depends on
   *   the managed object.
   * 
   * @param focus
   * @return
   */
  protected Focus<?> bindExports(Focus<?> focus)
    throws BindException
  { return focus;
  }
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    boolean httpPushed=false;
    // XXX Implement a pattern include and exclude list
    
    // log.fine("doFilter()");
    Focus<?> requestFocus=null;
    boolean pushed=false;
    try
    {
      // Grab our 'parent' Focus from the request (which should be stable)
      requestFocus=(Focus<?>) request.getAttribute(attributeName);
      // log.fine("Got "+requestFocus);
      
      if (!initialized)
      { 
        synchronized (this)
        { 
          if (!initialized)
          { init(requestFocus);
          }
        }
      }
      

      
      // Make our Focus the next filter's parent Focus
      request.setAttribute(attributeName,exportFocus);
      
      if (httpFocus!=null)
      { 
        httpFocus.push
          (config.getServletContext()
          ,(HttpServletRequest) request
          ,(HttpServletResponse) response
          );
        httpPushed=true;
      }
      
      
      if (whenX==null || Boolean.TRUE.equals(whenX.get()))
      {
        // Perform the filter function
        
        // Make sure the subject of our Focus is appropriate for this
        //   Thread's service operation for this request
        pushSubject((HttpServletRequest) request,(HttpServletResponse) response);
      
        pushed=true;
      
        if (renderer!=null
            && (renderWhenX==null || Boolean.TRUE.equals(renderWhenX.get())
               )
           )
        { 
          if (responseCodeX!=null)
          { ((HttpServletResponse) response).setStatus(responseCodeX.get());
          }
          if (contentTypeX!=null)
          { response.setContentType(contentTypeX.get());
          }
          renderer.render(response.getWriter());
        }
        else
        { 
          doChain
            (chain
            ,(HttpServletRequest) request
            ,(HttpServletResponse) response
            );
        }
      }
      else
      { 
        // Bypass the filter function
        chain.doFilter(request,response);
      }
    }
    catch (ContextualException x)
    { 
      ServletException sx=new ServletException(x.toString());
      sx.initCause(x);
      throw sx;
    }
    finally
    { 
      if (pushed)
      { 
        // If we changed this Thread's Focus subject, put it back
        popSubject((HttpServletRequest) request);
      }
      if (httpFocus!=null && httpPushed)
      { httpFocus.pop();
      }
      
      // Put the original focus back
      request.setAttribute(attributeName,requestFocus);
    }
    
  }
  
  /**
   * Override to perform additional operations in context and control 
   *   the execution of the rest of the filter.
   * 
   * @param chain
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  protected void doChain
    (FilterChain chain
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
    throws ServletException,IOException
  { chain.doFilter(request,response);
  }
  
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private final void init(Focus<?> requestFocus)
    throws ContextualException
  {
  
    
    // Create our own Focus, using the 'parent' Focus.
    if (focus==null)
    {         
      if (requestFocus==null)
      { requestFocus=new SimpleFocus<Void>(null);
      }
      
      if (usesRequest 
          && requestFocus.findFocus
            (URI.create("class:/javax/servlet/http/HttpServletRequest")
            ) ==null
         )
      { 
        httpFocus=new HttpFocus<Void>(requestFocus);
        requestFocus=httpFocus;
      }
      if (whenX!=null)
      { whenX.bind(requestFocus);
      }
      requestFocus=bindImports(requestFocus);
      focus=createFocus(requestFocus);
      // log.fine("Created "+focus);
      if (alias!=null)
      { focus.addAlias(alias);
      }
      exportFocus=bindExports(focus);
      
      if (renderWhenX!=null)
      { renderWhenX.bind(exportFocus);
      }
      if (contentTypeX!=null)
      { contentTypeX.bind(exportFocus);
      }
      if (responseCodeX!=null)
      { responseCodeX.bind(exportFocus);
      }
      
      Focus<?> renderFocus=focus;
      if (outputX!=null)
      {
        if (renderer==null)
        { renderer=new ExpressionRenderer(outputX);
        }
        else
        { renderFocus=renderFocus.chain(renderFocus.bind(outputX));
        }
      }
      
      if (renderer!=null && renderer instanceof Contextual)
      { ((Contextual) renderer).bind(renderFocus);
      }        
    }
  }
  
}
