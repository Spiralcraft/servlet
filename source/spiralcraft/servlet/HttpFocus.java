//
// Copyright (c) 1998,2007 Michael Toth
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

import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;

import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.reflect.BeanReflector;

import javax.servlet.ServletContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <P>Exposes HTTP/Servlet container intrinsics to the spiralcraft.lang 
 *   expression language when used in a Servlet context. 
 *   
 *   
 * <P>The <CODE>[http:Session]</CODE> Focus has the 
 *   javax.servlet.http.HttpSession object as its subject 
 *   (eg. <code>[http:Session] .id</code>), and an arbitrary namespace scoped
 *   to the this session as its context 
 *   (eg. <code>[http:Session] myAttribute</code>)
 * 
 * <P>The <CODE>[http:Application]</CODE> Focus has the
 *   javax.servlet.ServletContext object as its subject
 *   (eg. <code>[http:Application] .serverInfo</code>), and an arbitrary
 *   namespace scoped to the application as its context
 *   (eg. <code>[http:Application] myAttribute</code>)
 *
 * <P>The <CODE>[http:Request]</CODE> Focus has the
 *   javax.servlet.http.HttpServletRequest object as its subject and context
 *   (eg. <code>[http:Request] requestURI</code>)
 * 
 * <P>The <CODE>[http:Response]</CODE> Focus has the
 *   javax.servlet.http.HttpServletResponse object as its subject and context
 *   (eg. <code>[http:Response] .setContentType("xyz")</code>)
 * 
 * 
 * @author mike
 */
public class HttpFocus<T>
  extends SimpleFocus<T>
{

  private ThreadLocalChannel<ServletContext> servletContextBinding;
  private ThreadLocalChannel<HttpSession> sessionBinding;
  private ThreadLocalChannel<HttpServletRequest> requestBinding;
  private ThreadLocalChannel<HttpServletResponse> responseBinding;

  
  public HttpFocus(Focus<?> focusChain)
  { 
    this();
    setParentFocus(focusChain);
  }
  
  public HttpFocus()
  {
    // addNamespaceAlias("http");
    
    servletContextBinding=new ThreadLocalChannel<ServletContext>
      (BeanReflector.<ServletContext>getInstance(ServletContext.class));
    
    SimpleFocus<ServletContext> servletContextFocus
      =new SimpleFocus<ServletContext>(servletContextBinding);
    addFacet(servletContextFocus);
    
    
    sessionBinding=new ThreadLocalChannel<HttpSession>
      (BeanReflector.<HttpSession>getInstance(HttpSession.class));
    SimpleFocus<HttpSession> sessionFocus
      =new SimpleFocus<HttpSession>(sessionBinding);
    addFacet(sessionFocus);
    
    requestBinding=new ThreadLocalChannel<HttpServletRequest>
      (BeanReflector.<HttpServletRequest>getInstance(HttpServletRequest.class));
    SimpleFocus<HttpServletRequest> requestFocus
      =new SimpleFocus<HttpServletRequest>(requestBinding);
    addFacet
      (requestFocus
      );

    responseBinding=new ThreadLocalChannel<HttpServletResponse>
      (BeanReflector.<HttpServletResponse>getInstance(HttpServletResponse.class));
    SimpleFocus<HttpServletResponse> responseFocus
      =new SimpleFocus<HttpServletResponse>(responseBinding);
    addFacet(responseFocus);
  }
  
  
  /**
   * <P>Put the intrinsics for the specified request/response pair into thread
   *   local storage for the current thread context for later retrieval via 
   *   request processing functionality.
   * 
   * <P>This method should be called from within a <code>try{}</code> block,
   *   where <code>pop()</code> is called from within the associated
   *   <code>finally{}</code> block.
   * 
   * @param servlet
   * @param request
   * @param response
   */
  public void push
    (ServletContext context
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
  {
    servletContextBinding.push(context);
    sessionBinding.push(request.getSession(false));
    requestBinding.push(request);
    responseBinding.push(response);
  }
  
  /**
   * <P>Remove the intrinsics for the specified request/response pair from thread
   *   local storage for the current thread context.
   */
  public void pop()
  {
    servletContextBinding.pop();
    sessionBinding.pop();
    requestBinding.pop();
    responseBinding.pop();
  }
  
}
