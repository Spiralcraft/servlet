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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.spi.ThreadLocalChannel;


/**
 * <p>An abstract class for publishing arbitrary objects into the 
 *   focus request chain.
 * </p>
 * 
 * @author mike
 *
 */
public abstract class RequestFocusFilter<T>
  extends FocusFilter<T>
{
  
  
  private ThreadLocalChannel<T> channel;
  private Reflector<T> reflector;
  
  
  protected abstract Reflector<T> resolveReflector(Focus<?> parentFocus);
  
  protected abstract T createValue
    (HttpServletRequest request,HttpServletResponse response);

  

  
  /**
   * <p>Called when the specified object is maide available in the Focus chain
   *   immediately before the resumption of request processing
   * </p>
   */
  protected void objectAvailable()
  {
  }

  /**
   * Called -once- to create the Focus
   */
  @Override
  protected Focus<T> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    reflector=resolveReflector(parentFocus);

    channel
      =new ThreadLocalChannel<T>
        (reflector);
    return parentFocus.chain(channel);
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  { channel.pop();
  }
  
  
  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
    throws BindException
  {
    T value=createValue(request,response);
    channel.push(value);
    if (debug)
    { 
      log.fine
        ("New value created: "
        +value+"("+reflector.getTypeURI()+")"
        );
    }
    objectAvailable();
    
  }

}
