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
package spiralcraft.servlet.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import java.io.IOException;

import spiralcraft.security.auth.Credential;

/**
 * Implements an HTTP-Authentication protocol variant, which involves a 
 *   challenge and response.
 * 
 * @author mike
 */
public interface HttpAdapter
{
  /**
   * <P>Specify the realm that this authenticator is authenticating for. 
   * 
   * <P>This value should be well-known, very stable, and preferably
   *   expressed in some "canonical" form.
   *   
   * <P>As this value is a factor in authentication cryptography, any change
   *   to it may invalidate passwords in the realm stored as part of digests.
   * 
   */
  void setRealm(String realm);
  
  /**
   * Read authorization info from the client
   * 
   * @param request The HttpServletRequest to read
   * @return An array of Credentials read from the client
   * @throws IOException
   * @throws ServletException
   */
  Credential<?>[] readAuthorization(HttpServletRequest request)
    throws IOException,ServletException;
  
  /**
   * Challenge the client to provide authorization info
   * 
   * @param response The HttpServletResponse to write
   * @throws IOException
   * @throws ServletException
   */
  void writeChallenge(HttpServletResponse response)
    throws IOException,ServletException;
}
