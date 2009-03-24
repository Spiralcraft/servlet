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

import java.io.IOException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;

import spiralcraft.codec.text.Base64Codec;
import spiralcraft.codec.CodecException;

import spiralcraft.log.ClassLog;
import spiralcraft.security.auth.Credential;
import spiralcraft.security.auth.UsernameCredential;
import spiralcraft.security.auth.PasswordCleartextCredential;


/**
 * Provides low level HTTP Authentication functionality using the "Basic"
 *   authentication method
 * 
 * @author mike
 */
public class BasicHttpAdapter
  implements HttpAdapter
{

  private static final ClassLog log
    =ClassLog.getInstance(BasicHttpAdapter.class);
  
  private String realm;

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
  public void setRealm(String realm)
  { this.realm=realm;
  }
    
  public Credential<?>[] readAuthorization(HttpServletRequest request)
    throws IOException,ServletException
  {
    // Get encoded username and password 
    String headerValue = request.getHeader("Authorization");
    String encodedCredentials=null;
    if (headerValue!=null)
    {
      // XXX Support other auth methods, ie. md5 digest
      
      
      // remove "Basic " from header value
      encodedCredentials = headerValue.substring(6);
    }
    //System.out.println("Authorization header= "+encodedCredentials);
    if (encodedCredentials!=null && encodedCredentials.length()>0)
    {
      ByteArrayInputStream in
        =new ByteArrayInputStream(encodedCredentials.getBytes());
      ByteArrayOutputStream out
        =new ByteArrayOutputStream();
      
      try
      { new Base64Codec().decode(in,out);
      }
      catch (CodecException x)
      { throw new ServletException(x.toString());
      }
      
      String decodedCredentials=new String(out.toByteArray());
      int separator=decodedCredentials.lastIndexOf(":");
      if (separator<0)
      { 
        log.info
          ("Credentials didn't contain ':' separator: "+encodedCredentials+"->"+decodedCredentials);
        return null;
      }
      String user 
        = decodedCredentials.substring
          (0, decodedCredentials.lastIndexOf(":"));
      String password 
        = decodedCredentials.substring
          (decodedCredentials.lastIndexOf(":")+1);
      
      return new Credential[]
        {new UsernameCredential(user)
        ,new PasswordCleartextCredential(password)
        };
    }
    return null;
  }
  
  
  public void writeChallenge(HttpServletResponse response)
    throws IOException
  {
    response.setHeader("WWW-Authenticate", "Basic realm=\""+realm+"\"");
    response.sendError(401);
  }
}
