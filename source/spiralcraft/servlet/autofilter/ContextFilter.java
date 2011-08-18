//
//Copyright (c) 1998,2011 Michael Toth
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

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Context;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.autofilter.spi.FocusFilter;

/**
 * <p>Publishes a Context Focus into the Focus chain
 * </p>
 * 
 */
public class ContextFilter<T>
  extends FocusFilter<T>
{

  private Context context;

  public void setContext(Context context)
  { this.context=context;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Focus<T> createFocus(
    Focus<?> parentFocus)
    throws ContextualException
  { return (Focus<T>) context.bind(parentFocus);
  }

  @Override
  protected void pushSubject(
    HttpServletRequest request,
    HttpServletResponse response)
    throws ContextualException,
    ServletException
  { context.push();
  }

  @Override
  protected void popSubject(
    HttpServletRequest request)
  { context.pop();
  }
  
}
