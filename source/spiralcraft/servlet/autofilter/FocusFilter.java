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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * <P>Provides a spiralcraft.lang Expression Language Focus to servlet API
 *    request processing components. 
 *    
 * <P>The Focus is passed via a ServletRequest attribute. It has the
 *    same lifecycle as the FocusFilter itself.
 */
public abstract class FocusFilter<T>
  extends AutoFilter
{
  private static final String attributeName="spiralcraft.lang.focus";
  
  private Focus<?> focus;
  private String namespace;
  private String name;
  
  public void setNamespace(String namespace)
  { this.namespace=namespace;
  }
  
  public void setName(String name)
  { this.name=name;
  }
  
  /**
   * Obtain the Focus associated with the deepest FocusFilter in the
   *   stack.
   * 
   * @param request
   * @return
   */
  public static Focus<?> getFocusChain(HttpServletRequest request)
  { return (Focus<?>) request.getAttribute(attributeName);
  }


  /**
   * Create a new instance of the Focus that will be inserted into the Focus
   *   chain (scoped to the lifetime of the FocusFilter).
   */
  protected abstract Focus<?> createFocus
    (Focus<?> parentFocus,String namespace,String name)
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
    Focus<?> requestFocus=null;
    boolean pushed=false;
    try
    {
      requestFocus=(Focus<?>) request.getAttribute(attributeName);
      if (focus==null)
      { focus=createFocus(requestFocus,namespace, name);
      }
      
      pushSubject((HttpServletRequest) request);
      pushed=true;
      request.setAttribute(attributeName,focus);
      // System.err.println("FocusFilter.doFilter");
      chain.doFilter(request,response);
    }
    catch (BindException x)
    { throw new ServletException(x.toString(),x);
    }
    finally
    { 
      if (pushed)
      { popSubject((HttpServletRequest) request);
      }
      request.setAttribute(attributeName,requestFocus);
    }
    
  }


  @Override
  public Class<? extends AutoFilter> getCommonType()
  { return FocusFilter.class;
  }


  @Override
  public void setParentInstance(AutoFilter parentInstance)
  {    
  }
}
