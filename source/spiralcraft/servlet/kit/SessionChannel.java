//
// Copyright (c) 1998,2011 Michael Toth
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
package spiralcraft.servlet.kit;

import javax.servlet.http.HttpSession;

import spiralcraft.lang.AccessException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.spi.AbstractChannel;

/**
 * Binds data to a Session attribute
 * 
 * @author mike
 *
 */
public class SessionChannel<T>
  extends AbstractChannel<T>
{
  
  private Channel<HttpSession> sessionX;
  private String attributeName;
  
  public SessionChannel
    (Reflector<T> reflector
    ,Channel<HttpSession> sessionX
    ,String attributeName
    )
  {
    super(reflector);
    this.sessionX=sessionX;
    this.attributeName=attributeName;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected T retrieve()
  { 
    HttpSession session=sessionX.get();
    if (session==null)
    { return null;
    }
    return (T) session.getAttribute(attributeName);
  }

  @Override
  protected boolean store(Object val)
    throws AccessException
  { 
    HttpSession session=sessionX.get();
    if (session!=null)
    { 
      session.setAttribute(attributeName,val);
      return true;
    }
    else
    { return false;
    }
    
  }

}
