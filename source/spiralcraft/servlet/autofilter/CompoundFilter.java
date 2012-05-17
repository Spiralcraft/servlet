//
// Copyright (c) 2012 Michael Toth
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
package spiralcraft.servlet.autofilter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import spiralcraft.servlet.util.LinkedFilterChain;
import spiralcraft.util.Path;
import spiralcraft.vfs.Resource;

/**
 * Aggregates a set of filters into a single filter
 * 
 * @author mike
 *
 */
public class CompoundFilter
  extends AutoFilter
{
  
  private AutoFilter[] filters;

  public CompoundFilter(AutoFilter[] filters)
  { this.filters=filters;
  }
  
  @Override
  public void init(FilterConfig config)
    throws ServletException
  { 
    super.init(config);
    for (Filter filter:filters)
    { filter.init(config);
    }
  }
  
  @Override
  void setPath(Path path)
  {
    super.setPath(path);
    for (AutoFilter filter:filters)
    { filter.setPath(path);
    }
  }
 
  @Override
  public void setGlobal(boolean global)
  { super.setGlobal(global);
  }
  
  @Override
  public void setPattern(String pattern)
  {
    super.setPattern(pattern);
    for (AutoFilter filter:filters)
    { filter.setPattern(pattern);
    }
  }
  
  @Override
  void setContainer(Resource container)
  { 
    super.setContainer(container);
    for (AutoFilter filter:filters)
    { filter.setContainer(container);
    }
  }
  
  @Override
  public void destroy()
  {
    for (Filter filter:filters)
    { filter.destroy();
    }
  }

  @Override
  public void doFilter(
    ServletRequest request,
    ServletResponse response,
    FilterChain chain)
    throws IOException,
    ServletException
  {
    FilterChain newChain=new LinkedFilterChain(filters,chain);
    newChain.doFilter(request,response);    
  }
}