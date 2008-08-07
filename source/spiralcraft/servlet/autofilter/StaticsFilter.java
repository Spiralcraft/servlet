//
//Copyright (c) 1998,2008 Michael Toth
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

import spiralcraft.lang.BindException;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.StaticFocus;


/**
 * <p>Exposes static methods from selected classes into the Focus chain
 * </p>
 */
public class StaticsFilter
  extends FocusFilter<Void>
{

  private Class<?>[] classes;

  /**
   * <p>Specify the set of Classes on which to expose static methods
   * </p>
   * 
   * @param classes
   */
  public void setClasses(Class<?>[] classes)
  { this.classes=classes;
  }
  
  /**
   * Called -once- to create the Focus
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Focus<Void> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    CompoundFocus<Void> ret=new CompoundFocus<Void>(parentFocus,null);
    
    if (classes!=null)
    {
      for (Class<?> clazz: classes)
      { ret.bindFocus(null,new StaticFocus(ret,clazz));
      }
    }
    return ret;
  }




  @Override
  protected void popSubject(HttpServletRequest request)
  {
    
  }
  
  

  @Override
  protected void pushSubject(HttpServletRequest request) 
    throws BindException
  {

  }


}

