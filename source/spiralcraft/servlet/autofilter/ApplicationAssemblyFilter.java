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

import javax.servlet.ServletContext;

import spiralcraft.lang.BindException;

import spiralcraft.lang.Focus;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Channel;


/**
 * Installs a Focus as an application attribute and appends reference to
 *   that Focus into the Focus chain available to request processing components
 *   
 * @author mike
 *
 * @param <F>
 * @param <C>
 */
public class ApplicationAssemblyFilter<F,C>
    extends AssemblyFilter<F>
{

  private String applicationAttributeName;
  
  public void setApplicationAttributeName(String name)
  { applicationAttributeName=name;
  }
  
  @SuppressWarnings("unchecked") // We don't know what type the context is
  @Override
  protected Focus<?> createFocus
    (Focus<?> parentFocus, String namespace, String name)
    throws BindException
  { 

    ServletContext context=this.config.getServletContext();
    
    FocusHolder targetFocusHolder
      =(FocusHolder) context.getAttribute(applicationAttributeName);
   
    if (targetFocusHolder==null)
    { 
      targetFocusHolder=new FocusHolder();
      context.setAttribute(applicationAttributeName, targetFocusHolder);
    }

   
    Channel<F> subjectBinding
      =targetFocusHolder.getFocus().getSubject();
    
    return new CompoundFocus<F>
      (parentFocus,subjectBinding);

  }

  @SuppressWarnings("unchecked") // Cast session attribute, unknown context type
  @Override
  protected void pushSubject(HttpServletRequest request)
    throws BindException
  {

  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  {
    
  }


}
