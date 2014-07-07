//
//Copyright (c) 2012 Michael Toth
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
package spiralcraft.servlet.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;

public class Session
{

  protected static final ClassLog log
    =ClassLog.getInstance(Session.class);
 
  private String systemId;
  private String token;
  private String authStateDigest;
  private String extId;
  private String extData;
  
  private boolean temporary;
  protected Level logLevel=Level.INFO;
  private String problem;
  
  protected Session()
  { 
  }

  
  /**
   * The unique and persistent id associated with
   *   the external system account that this 
   *   session is authenticated with, if any. This may be the end user's own 
   *   user id on the server, or it may be the id of the resource owner.
   * 
   * @return
   */
  public String getToken()
  { return token;
  }
  
  public String getSystemId()
  { return systemId;
  }
  
  /**
   * An external-system specific id for use when associating new
   *   credentials with logins
   * 
   * @param extId
   */
  public void setExtId(String extId)
  { this.extId=extId;
  }

  public String getExtId()
  { return extId;
  }
  
  /**
   * External-system specific data for use when associating new
   *   credentials with logins
   *   
   * @return
   */
  public void setExtData(String extData)
  { this.extData=extData;
  }

  public String getExtData()
  { return extData;
  }
  
  public boolean isTokenValid()
  { 
    // Incorp timeout
    return token!=null && !temporary && problem==null;
  }
  
  public void applyCredentials(String systemId,String token)
  {
    this.systemId=systemId;
    this.token=token;
    this.authStateDigest=Integer.toString((systemId+token).hashCode());
  }
  
  /**
   * 
   * @return A digest of the authentication credentials used to detect when
   *   credentials have changed and trigger re-authentication.
   */
  public String getAuthStateDigest()
  { return authStateDigest;
  }
  
  public boolean getCredentialsPresent()
  { return token!=null && systemId!=null;
  }
  
  public String getProblem()
  { return problem;
  }
  
  public void abortAuthSequence(String failureCode)
  { this.problem=failureCode;
  }
  

  /**
   * <p>Perform any actions required after successfully authenticating, such
   *   as populating the principalId property.
   * </p>
   * 
   * @throws IOException
   * @throws GeneralSecurityException
   */
  protected void postAuthenticate()
    throws IOException,GeneralSecurityException
  {
  }
  
  public void invalidate()
    throws IOException
  { 
    clear();
  }
  
  public void clear()
  {
    token=null;
    systemId=null;
    temporary=false;
    problem=null;
  }
}
