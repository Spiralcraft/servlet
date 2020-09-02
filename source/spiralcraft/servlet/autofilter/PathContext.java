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
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.Level;
import spiralcraft.servlet.kit.UIResourceMapping;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.string.StringUtil;
import spiralcraft.util.thread.ThreadLocalStack;
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
  private static final ThreadLocalStack<PathContext> instance
    =new ThreadLocalStack<PathContext>();

  public static final PathContext instance()
  { return instance.get();
  }
  
  private AutoFilter[] preFilters;
  private AutoFilter[] filters;
  private Channel<HttpServletRequest> requestChannel;
  // private PathContext parent;
  private final HashMap<String,PathContextFilter> filterMap
    =new HashMap<String,PathContextFilter>();
  private Binding<UIResourceMapping> resourceMappingX;
  private String indexResource=null;
  private String defaultResource="default.webui";
  private boolean handleAllRequests;
  private boolean mapWebUIResources=true;

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
   * An expression which maps the current request to a UIResourceMapping (a
   *   named instance of a component created from a specified definition
   *   resource).
   * 
   * @param resourceMappingX
   */
  public void setResourceMappingX(Binding<UIResourceMapping> resourceMappingX)
  { this.resourceMappingX=resourceMappingX;
  }
  
  /**
   * <p>Specifies that all requests passing through this PathContext will be
   *   handled directly by resources associated with this PathContext, instead
   *   of being delegated to a child PathContext.
   * </p>
   * <p>
   *  The resource can access any remaining path information via the "pathInfo"
   *   property.
   * </p>
   * 
   * @param handleAllRequests
   */
  public void setHandleAllRequests(boolean handleAllRequests)
  { this.handleAllRequests=handleAllRequests;
  }

  /**
   * Whether to automatically use the .webui files in this PathContext's directory
   *   that has the same name as the next path segment in the request.
   *   
   * This is the default behavior (true).
   *    
   * @param mapWebuiResources
   */
  public void setMapWebuiResources(boolean mapWebuiResources)
  { this.mapWebUIResources=mapWebuiResources;
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
  
  /**
   * Specify a resource to handle a request that has no path info (a request
   *   directly to this path context) when handleAllRequests=true.
   * 
   * @param indexResource
   * @return
   */
  public void setIndexResource(String indexResource)
  { this.indexResource=indexResource;
  }
  
  /**
   * Specify the resource that will handle all requests not otherwise mapped
   *   to a handler, when handleAllRequests=true
   *       
   * @param indexResource
   * @return
   */
  public void setDefaultResource(String defaultResource)
  { this.defaultResource=defaultResource;
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
    AutoFilter filter=filterMap.get(path);
    if (filter==null && !filterMap.containsKey(path))
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
    if (!filterMap.containsKey(path))
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
          filter.setDebug(logLevel.isDebug());
          filter.setPath(this.getAbsolutePath().append(path));
          filter.setContainer
            (Resolver.getInstance().resolve
              (getEffectiveCodeBaseURI().resolve(contextURI))
            );
          filter.setCodeSearchRoot(getEffectiveCodeBaseURI().resolve(contextURI));
          filter.init(config);
        }
        filterMap.put(path,filter);
      }
    }
    else
    { filter=filterMap.get(path);
    }
    return filter;
  }
  
  
  @Override
  protected Focus<?> bindImports(Focus<?> chain)
    throws ContextualException
  { 
    requestChannel=LangUtil.assertChannel(HttpServletRequest.class,chain);
    return super.bindImports(chain);
  }
  
  @Override
  protected Focus<?> bindExports(Focus<?> chain) 
    throws ContextualException
  { 
    chain=super.bindExports(chain);
    if (resourceMappingX!=null)
    { resourceMappingX.bind(chain);
    }
    return chain;
  }
  
  public UIResourceMapping 
    createResourceMapping(String pathInfo,String resourceURI)
    throws IOException
  { 
    return UIResourceMapping.forResource
      (getAbsolutePathString()+pathInfo
      ,this.resolveCode(resourceURI)
      );
  }
    
  private boolean validRelativeURI(String pathSegment)
  {
    try
    { return !new URI(pathSegment).isAbsolute();
    }
    catch (URISyntaxException x)
    { 
      log.info("Bad pathinfo: "+pathSegment+" in "+getAbsolutePathString());
      return false;
    }
  }
  
  /**
   * 
   * @return The resource that will handle the current request, if one has
   *   been determined.
   */
  public UIResourceMapping uiResourceForRequest()
    throws IOException
  { 
    try
    {
      String nextPathInfo=getNextPathInfo();
      

      UIResourceMapping resource;
      if (mapWebUIResources && validRelativeURI(nextPathInfo))
      {
        resource
          =UIResourceMapping.forResource
            (getAbsolutePathString()+nextPathInfo
            ,this.resolveCode(nextPathInfo+".webui")
            );
        if (resource!=null)
        { return resource;
        }
      }
    
      if (resourceMappingX!=null)
      { 
        resource=resourceMappingX.get();
        if (resource!=null)
        { return resource;
        }
      }
      
      if (handleAllRequests)
      { 
        UIResourceMapping ret=null;
        if (indexResource!=null)
        {
          if (nextPathInfo==null || nextPathInfo.isEmpty())
          { ret=UIResourceMapping.forResource
              (getAbsolutePathString(),this.resolveCode(indexResource));
          }
          else
          { ret=UIResourceMapping.forResource
              (getAbsolutePathString()+"*",this.resolveCode(defaultResource));
          }
        }
        else
        { 
          ret=UIResourceMapping.forResource
            (getAbsolutePathString(),this.resolveCode(defaultResource));
        }
        return ret;
      }
      return null;
    }
    catch (IOException x)
    { 
      log.log(Level.DEBUG,x.toString(),x);
      throw x;
    }
  }
  
  public String getStaticRequestPath()
  { return getPathInfo();
  }
  
  public String getDynamicRequestPath()
  { return null;
  }
  
  @Override
  protected void pushLocal()
  {
    super.pushLocal();
    instance.push(this);
    if (logLevel.isFine())
    { log.fine("Entering pathContext "+getAbsolutePath());
    }
    // TODO: Analyze the request path and split into static/dynamic path components
    //   then make available for the handling resource
  }
  
  @Override
  protected void popLocal()
  {
    if (logLevel.isFine())
    { log.fine("Exiting pathContext "+getAbsolutePath());
    }
    instance.pop();
    // TODO: Pop the request status from thread local
    super.popLocal();
  }

}
