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
import javax.servlet.http.HttpSession;


import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.spi.ThreadLocalChannel;


/**
 * <p>An abstract class for making session-scoped objects available in 
 *   the Focus chain
 * </p>
 * 
 * @author mike
 *
 */
public abstract class SessionFocusFilter<T>
  extends FocusFilter<T>
{
  
  
  private ThreadLocalChannel<T> sessionChannel;
  private String attributeName;
  private Reflector<T> reflector;
  
  
  protected abstract Reflector<T> resolveReflector(Focus<?> parentFocus);
  
  protected abstract T createValue
    (HttpServletRequest request,HttpServletResponse response);


  /**
   * Override to access existing session object via the 
   *   Focus immediately before continuation of request processing. 
   */
  protected void sessionObjectAvailable(boolean created)
  {
  }
  
  protected boolean isSessionObjectUpdated()
  { return false;
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
    
    this.attributeName=this.getPath().format("/")+"!"+
      reflector.getTypeURI();
      
    // XXX Replace with XML binding
    sessionChannel
      =new ThreadLocalChannel<T>
        (reflector);
    
    return parentFocus.chain(sessionChannel);
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  { 

    if (isSessionObjectUpdated())
    { request.getSession().setAttribute(attributeName, sessionChannel.get());
    }
    sessionChannel.pop();
  }
  
  

  @SuppressWarnings("unchecked")
  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
    throws BindException
  {
      
    HttpSession session=request.getSession();
    T sessionValue = (T) session.getAttribute(attributeName);
      
    boolean newSession=false;
    if (sessionValue==null)
    { 
      // Avoid race condition
      synchronized (session)
      {
        sessionValue
          =(T) session.getAttribute(attributeName);
        if (sessionValue==null)
        {
          newSession=true;
          sessionValue=createValue(request,response);
          session.setAttribute(attributeName,sessionValue);
          if (debug)
          { 
            log.fine
              ("New sessionValue created: "
              +sessionValue+"("+reflector.getTypeURI()+")"
              +" in http session "+session.getId()
              );
          }
          
        }
        else
        {
          if (debug)
          { 
            log.fine
              ("Race condition averted for sessionValue "
              +sessionValue+"("+reflector.getTypeURI()+")"
              +" in http session "+session.getId()
              );
          }
        }
      }  
    }
    sessionChannel.push(sessionValue);  

    sessionObjectAvailable(newSession);
    
  }

}
