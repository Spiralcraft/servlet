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

import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.servlet.autofilter.AutoFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Provides a spiralcraft.lang Focus to servlet API request processing 
 *   components. 
 * </p>
 *    
 * <p>The Focus is passed via a ServletRequest attribute. It has the
 *    same lifecycle as the FocusFilter itself, and is permanently referenced
 *    by the FocusFilter instance.
 * </p>
 */
public abstract class FocusFilter<T>
  extends AutoFilter
{
  
  private static final String attributeName="spiralcraft.lang.focus";
  
  private Focus<T> focus;
  private Focus<?> exportFocus;
  private URI alias;
  
  // Default to global, to implement Focus hierarchy
  { setGlobal(true);
  }
  
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
   * </p>
   * 
   * @param request 
   * @return The Focus
   */
  public static Focus<?> getFocusChain(HttpServletRequest request)
  { return (Focus<?>) request.getAttribute(attributeName);
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
  public void setAlias(URI alias)
  { this.alias=alias;
  }
  
  /**
   * Create a new instance of the Focus that will be inserted into the Focus
   *   chain (scoped to the lifetime of the FocusFilter). This method will
   *   be called only once.
   */
  protected abstract Focus<T> createFocus
    (Focus<?> parentFocus)
    throws BindException;
  
  /**
   * Populate the subject of the Focus with the appropriate object for
   *   the Thread processing this request.
   *   
   * @param request
   */
  protected abstract void pushSubject
    (HttpServletRequest request,HttpServletResponse response)
    throws BindException;
  
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
  { return focus;
  }
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    // XXX Implement a pattern include and exclude list
    
    // log.fine("doFilter()");
    Focus<?> requestFocus=null;
    boolean pushed=false;
    try
    {
      // Grab our 'parent' Focus from the request (which should be stable)
      requestFocus=(Focus<?>) request.getAttribute(attributeName);
      // log.fine("Got "+requestFocus);
      

      
      // Create our own Focus, using the 'parent' Focus.
      if (focus==null)
      {         
        if (requestFocus==null)
        { requestFocus=new SimpleFocus<Void>(null);
        }
        requestFocus=bindImports(requestFocus);
        focus=createFocus(requestFocus);
        // log.fine("Created "+focus);
        if (alias!=null)
        { focus.addAlias(alias);
        }
        exportFocus=bindExports(focus);
      }
      
      // Make sure the subject of our Focus is appropriate for this
      //   Thread's service operation for this request
      pushSubject((HttpServletRequest) request,(HttpServletResponse) response);
      
      pushed=true;
      
      // Make our Focus the next filter's parent Focus
      request.setAttribute(attributeName,exportFocus);
      // log.fine("Setting "+focus);
      
      // System.err.println("FocusFilter.doFilter");
      chain.doFilter(request,response);
    }
    catch (BindException x)
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
//      log.fine("Restoring "+requestFocus);
      // Put the original focus back
      request.setAttribute(attributeName,requestFocus);
    }
//    log.fine("/doFilter()");
    
  }


  @Override
  public Class<? extends AutoFilter> getCommonType()
  { return FocusFilter.class;
  }


  @Override
  public void setGeneralInstance(AutoFilter generalInstance)
  {    
  }
}
