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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import spiralcraft.lang.BindException;

import spiralcraft.lang.Focus;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.spi.ThreadLocalBinding;

/**
 * Installs a Focus as an HttpSession attribute and appends reference to
 *   that Focus into the Focus chain available to request processing components
 *   
 * @author mike
 *
 * @param <F>
 * @param <C>
 */
public class SessionAssemblyFilter<F,C>
    extends AssemblyFilter<F>
{

  private String sessionAttributeName;
  private ThreadLocalBinding<F> subjectBinding;

  
  public void setSessionAttributeName(String name)
  { sessionAttributeName=name;
  }
  
  @SuppressWarnings("unchecked") // We don't know what type the context is
  @Override
  protected Focus<?> createFocus
    (Focus<?> parentFocus, String namespace, String name)
    throws BindException
  { 
   
    FocusHolder targetFocusHolder
      =new FocusHolder();
    
    subjectBinding
      =new ThreadLocalBinding<F>
        (targetFocusHolder.getFocus().getSubject().getReflector());
    
    return new CompoundFocus<F>
      (parentFocus,subjectBinding);
  }


  @SuppressWarnings("unchecked") // Cast session attribute, unknown context type
  @Override
  protected void pushSubject(HttpServletRequest request)
    throws BindException
  {
    HttpSession session=request.getSession();
    FocusHolder targetFocusHolder
      =(FocusHolder) session.getAttribute(sessionAttributeName);
    
    if (targetFocusHolder==null)
    { 
      targetFocusHolder=new FocusHolder();
      session.setAttribute(sessionAttributeName, targetFocusHolder);
    }
    subjectBinding.push(targetFocusHolder.getFocus().getSubject().get());
    
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  {
    subjectBinding.pop();
    


  }


}
