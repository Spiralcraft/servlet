//
//Copyright (c) 2010 Michael Toth
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.servlet.autofilter.spi.RequestFocusFilter;

/**
 * Puts the result of an expression into the Focus chain
 * 
 * @author mike
 *
 */
public class ExpressionFocusFilter<T>
  extends RequestFocusFilter<T>
{
  
  public Binding<T> x;

  
  /**
   * Specify the Expression to bind
   * 
   * @param x
   */
  public void setX(Binding<T> x)
  { this.x=x;
  }
  
  @Override
  protected T createValue(
    HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException
  { return x.get();
  }

  @Override
  protected Reflector<T> resolveReflector(
    Focus<?> parentFocus)
    throws BindException
  { 
    x.bind(parentFocus);
    return x.getReflector();
  }

}
