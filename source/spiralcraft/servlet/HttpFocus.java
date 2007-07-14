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

import spiralcraft.lang.CompoundFocus;

/**
 * <P>Exposes HTTP/Servlet container intrinsics to the spiralcraft.lang 
 *   expression language when used in a Servlet context.
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
  extends CompoundFocus<T>
{

  
}
