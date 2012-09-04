//
//Copyright (c) 2012 Michael Toth
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


import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.string.StringUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.app.PathContextMapping;
import spiralcraft.common.ContextualException;

/**
 * <p>Defines the functionality of a subtree addressed by a URI path
 * </p>
 * 
 * @author mike
 *
 */
public class PathContext
  extends spiralcraft.app.PathContext
{

  private AutoFilter[] preFilters;
  private AutoFilter[] filters;
  private Channel<HttpServletRequest> requestChannel;
  // private PathContext parent;
  private final HashMap<String,PathContextFilter> pathMap
    =new HashMap<String,PathContextFilter>();

  /**
   * The filter chain to be used for all requests. The filters are chained
   *   within the scope of the PathContext, prior to invoking the
   *   external filter chain.
   * 
   * @param placeContext
   */
  public void setFilters(AutoFilter[] filters)
  { this.filters=filters;
  }
  
  /**
   * The filter chain to be used for all requests to provide context for the
   *   PathContext, chained prior to the PathContext itself.
   * 
   * @param placeContext
   */
  public void setPreFilters(AutoFilter[] filters)
  { this.preFilters=filters;
  }
  
  public AutoFilter[] getPreFilters()
  { 
    return ArrayUtil.concat
      (AutoFilter[].class
      ,preFilters
      ,(baseContext!=null && baseContext instanceof PathContext)
        ?((PathContext) baseContext).getPreFilters():null
      );
  }

  
  /**
   * <p>Return the contents of the current requestURI after the part that
   *   addresses the nearest containing PathContext. The path will never start
   *   with a leading slash.
   * </p>
   * 
   * @return
   */
  public String getPathInfo()
  { 
    HttpServletRequest request=requestChannel.get();
    return relativize
      (request.getServletPath()
        +(request.getPathInfo()!=null?request.getPathInfo():"")
      );
  }
  
  public String getNextPathInfo()
  {
    String pathInfo=getPathInfo();
    return StringUtil.discardAfter(pathInfo,'/');
  }
  
  public String getCanonicalRedirectPath(String contextRelativeRequestPath)
  {
    if (getAbsolutePathString().length()==contextRelativeRequestPath.length()+1
        && getAbsolutePathString().startsWith(contextRelativeRequestPath)
        )
    { return getAbsolutePathString();
    }
    else
    { return null;
    }
  }

  
  AutoFilter[] getFilters()
  { 
    return ArrayUtil.concat
      (AutoFilter[].class
      ,filters
      ,(baseContext!=null && baseContext instanceof PathContext)
        ?((PathContext) baseContext).getFilters():null
      );
    
  }
  
  /**
   * Get the filter chain that is specific to the request sub-path
   * 
   * @param request
   * @return
   */
  AutoFilter getRequestPathFilter
    (HttpServletRequest request
    ,FilterConfig parentConfig
    )
    throws IOException,ServletException
  {
    String path=getNextPathInfo();
    AutoFilter filter=pathMap.get(path);
    if (filter==null && !pathMap.containsKey(path))
    { 
      synchronized (this)
      { filter=mapFilter(path,parentConfig);
      }
    }
    return filter;
  }
  
  private AutoFilter mapFilter(String path,FilterConfig config)
    throws IOException,ServletException
  {
    PathContextFilter filter=null;
    if (!pathMap.containsKey(path))
    {
      if (pathMappings!=null)
      {
        URI contextURI=null;
        for (PathContextMapping mapping: pathMappings)
        { 
          if (mapping.matches(path))
          { contextURI=mapping.getContextURI();
          }
        }
        if (contextURI!=null)
        {
          filter=new PathContextFilter();
          filter.setPath(this.getAbsolutePath().append(path));
          filter.setContainer
            (Resolver.getInstance().resolve
              (getEffectiveCodeBaseURI().resolve(contextURI))
            );
          filter.setCodeSearchRoot(getEffectiveCodeBaseURI().resolve(contextURI));
          filter.init(config);
        }
        pathMap.put(path,filter);
      }
    }
    else
    { filter=pathMap.get(path);
    }
    return filter;
  }
  
  
  @Override
  protected Focus<?> bindImports(Focus<?> chain)
    throws ContextualException
  { 
    requestChannel=LangUtil.findChannel(HttpServletRequest.class,chain);
    return super.bindImports(chain);
  }

}
