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

import spiralcraft.lang.Focus;
import spiralcraft.lang.BindException;
import spiralcraft.log.ClassLogger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Provides a spiralcraft.lang Expression Language Focus to servlet API
 *    request processing components. 
 * </p>
 *    
 * <P>The Focus is passed via a ServletRequest attribute. It has the
 *    same lifecycle as the FocusFilter itself, and is permanently referenced
 *    by the FocusFilter instance.
 */
public abstract class FocusFilter<T>
  extends AutoFilter
{
  @SuppressWarnings("unused")
  private static final ClassLogger log=ClassLogger.getInstance(FocusFilter.class);
  
  private static final String attributeName="spiralcraft.lang.focus";
  
  private Focus<T> focus;
  
  // Default to global, to implement Focus hierarchy
  { setGlobal(true);
  }
  
  /**
   * <p>Obtain the Focus associated with the deepest FocusFilter in the
   *   stack.
   * </p>
   * 
   * XXX If we are building context duration, this should always return the
   *   same Focus, because the Filter object is also context duration. Perhaps
   *   the filter should just get the Focus from its parent.
   * 
   * @param request 
   * @return
   */
  public static Focus<?> getFocusChain(HttpServletRequest request)
  { return (Focus<?>) request.getAttribute(attributeName);
  }

  public Focus<T> getFocus()
  { return focus;
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
  protected abstract void pushSubject(HttpServletRequest request)
    throws BindException;
  
  /**
   * Restore the subject of the Focus with the object it referenced for this
   *   thread before the pushSubject() method was called.
   *   
   * @param request
   */
  protected abstract void popSubject(HttpServletRequest request);
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
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
        focus=createFocus(requestFocus);
        // log.fine("Created "+focus);
      }
      
      // Make sure the subject of our Focus is appropriate for this
      //   Thread's service operation for this request
      pushSubject((HttpServletRequest) request);
      
      pushed=true;
      
      // Make our Focus the next filter's parent Focus
      request.setAttribute(attributeName,focus);
      // log.fine("Setting "+focus);
      
      // System.err.println("FocusFilter.doFilter");
      chain.doFilter(request,response);
    }
    catch (BindException x)
    { throw new ServletException(x.toString(),x);
    }
    finally
    { 
      if (pushed)
      { 
        // If we changed this Thread's Focus subject, put it back
        popSubject((HttpServletRequest) request);
      }
//      log.fine("Restoring "+requestFocus);
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
