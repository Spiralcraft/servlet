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
package spiralcraft.servlet.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A simple FilterChain implementation based on a linked list pattern
 *
 */
public class LinkedFilterChain
  implements FilterChain
{

  private final Filter filter;
  private FilterChain next;
  
  public LinkedFilterChain(Filter filter)
  { this.filter=filter;
  }
  
  public LinkedFilterChain(Filter filter,FilterChain next)
  { 
    this.filter=filter;
    this.next=next;
  
  }

  /**
   * <P>Insert the LinkedFilterChain between this element and any configured
   *   next element
   */
  public void insert(LinkedFilterChain next)
  { 
    next.setNext(this.next);
    this.next=next;
  }
  
  /**
   * <P>Set the next element of the chain to be the specified FilterChain.
   * 
   * <P>If the next element has already been specified, it will be discarded.
   */
  public void setNext(FilterChain next)
  { this.next=next;
  }
  
  /**
   * Pass the request and response through the next filter
   */
  @Override
  public void doFilter
    (ServletRequest request
    ,ServletResponse response
    ) 
    throws IOException
      ,ServletException
  { filter.doFilter(request,response,next);
  }
}
