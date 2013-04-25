//
//Copyright (c) 2013 Michael Toth
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
package spiralcraft.servlet.webui;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.PublicLocator;
import spiralcraft.servlet.webui.kit.PortSession;

/**
 * Provides access to the state of the UI during the servicing of an HTTP
 *   request or other message.
 * 
 * @author mike
 *
 */
public interface UIContext
{
  
  /**
   * 
   * @return A map of the name-value pairs transmitted in the request body of
   *   an HTTP post, if any
   */
  VariableMap getPost();
  
  /**
   * 
   * @return A map of the name-value pairs in the query string of the
   *   request URI.
   */
  VariableMap getQuery();
  
  /**
   * Queues a redirect to the specified URI
   * 
   * @param uriStr
   * @throws URISyntaxException
   * @throws ServletException
   */
  void redirect(String uriStr)
    throws URISyntaxException,ServletException;
  
  /**
   * Queues a redirect to the specified URI
   * 
   * @param rawURI
   * @throws ServletException
   */
  void redirect(URI rawURI)
    throws ServletException;
  
  /**
   * 
   * @return A back-link to the containing page or port computed from the
   *   active HTTP servlet request.
   */
  public String getAbsoluteBackLink();
  
  /**
   * 
   * @return The PublicLocator associated with this app context that computes
   *   absolute standard and secure URLs into this app instance.
   */
  public PublicLocator getPublicLocator();

  /**
   * 
   * @return A back-link to the containing page or port computed from the
   *   active HTTP servlet request, and encoded for concatenation into
   *   a URI query string
   */
  public String getDataEncodedAbsoluteBackLink();
  
  /**
   * The URL that can be used to issue asynchronous requests back to this
   *   page. The URL reflects the current state of the conversation.
   *   
   * @return
   */
  public String getAsyncURL();
  
  /**
   * 
   * @return The active HttpServletResponse
   */
  public HttpServletResponse getResponse();

  /**
   * 
   * @return The active HttpServletRequest
   */
  public HttpServletRequest getRequest();
    
  /**
   * 
   * @return The ResourceSession that stores data and
   *   objects for a user's session that are associated with the 
   *   containing WebUI user interface resource
   */
  public PortSession getPortSession();

  
}
