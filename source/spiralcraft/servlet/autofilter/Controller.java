//
//Copyright (c) 1998,2012 Michael Toth
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

import spiralcraft.common.ContextualException;
import spiralcraft.common.LifecycleException;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.Focus;
import spiralcraft.log.Level;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.servlet.kit.InternalHttpServletRequest;
import spiralcraft.servlet.kit.InternalHttpServletResponse;
import spiralcraft.servlet.kit.StandardFilterConfig;
import spiralcraft.servlet.kit.WebApplicationContext;
import spiralcraft.servlet.util.LinkedFilterChain;
import spiralcraft.text.html.URLEncoder;
import spiralcraft.time.Clock;
import spiralcraft.util.Path;
import spiralcraft.util.URIUtil;
import spiralcraft.util.tree.PathTree;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.ResourceFilter;
import spiralcraft.vfs.UnresolvableURIException;
import spiralcraft.vfs.context.Graft;
import spiralcraft.vfs.file.FileResource;
import spiralcraft.vfs.ovl.OverlayResource;
import spiralcraft.vfs.Package;




/**
 * 
 * <p>Manages Filter chains for an HTTP resource (directory) tree using 
 *   definition files contained inside the resource tree. 
 * </p>
 * 
 * <p>One Controller is registered as a 'global' Filter for a given
 *   container context. The controller monitors the resource tree being
 *   served by the context and maintains sets of filters for specific
 *   paths as defined by the ".control.xml" control files found in the
 *   paths.
 * </p>
 *   
 * <p>The Controller will re-scan the resource tree for new or changed
 *   control files at a configurable interval that defaults to every 10
 *   seconds.
 * </p>
 */
public class Controller
  extends WebApplicationContext
  implements Filter,Contextual
{
  
  private int updateIntervalMs=10000;
  
  private final HashMap<String,CacheEntry> uriCache
    =new HashMap<String,CacheEntry>();
  
  private final PathTree<FilterSet> pathTree
    =new PathTree<FilterSet>(null);
  
  private FilterConfig config;
  private long lastUpdate;
  
  private Throwable throwable;
  private boolean showExceptions=false;
  private boolean tracePathResolution=false;
  
  private ResourceFilter exclusionFilter
    =new ResourceFilter()
  {

    @Override
    public boolean accept(Resource resource)
    {
      if (resource.getURI().getPath()==null)
      { return true;
      }
      Path path=Path.create(resource.getURI().getPath());
      if (path.lastElement().equals("CVS")
          || path.lastElement().equals(".svn")
          || path.lastElement().equals("WEB-INF")
         )
      { return false;
      }
      return true;
    }
  };

  
  private AppContextFilter appContextFilter
    =new AppContextFilter();
  { 
    appContextFilter.setGlobal(true);
    appContextFilter.setPattern("*");
  }
  
  
  /**
   * Filter.init()
   */
  @Override
  public void init(FilterConfig config)
    throws ServletException
  {
    String configId
      =config.getServletContext().getInitParameter("spiralcraft.config.id");
    if (configId!=null)
    { 
      this.configURI
        =URI.create("config."+configId+"/");
    }
    
    
    // Run autoconfig here
    
    this.config=new StandardFilterConfig(null,config.getServletContext(),null);
    if ("true".equals(config.getInitParameter("debug")))
    { debug=true;
    }
    
    
    
    ServletContext context=config.getServletContext();
    
    // Grab the container focus, if available
    focus=(Focus<?>) context.getAttribute("spiralcraft.lang.Focus");
    
    showExceptions
      ="true"
        .equals
          (context.getInitParameter
            ("spiralcraft.servlet.showExceptions")
          );
    String realPath=context.getRealPath("/");
    if (realPath!=null)
    { 
      publishRoot=new FileResource(new File(realPath));
      if (debug)
      { log.fine("Root is "+publishRoot.getURI());
      }
    }
    else
    { 
      URI publishURI=(URI) context.getAttribute("spiralcraft.context.root.uri");
      if (publishURI!=null)
      { 
        try
        { publishRoot=Resolver.getInstance().resolve(publishURI);
        }
        catch (UnresolvableURIException x)
        { throw new ServletException("Unable to resolve "+publishURI,x);
        }
      }
    }
    

    if (publishRoot==null)
    { 
      throw new ServletException
        ("Unable to determine publish root URI from ServletContext "
        +" (Context is not filesystem based and "
        +"'spiralcraft.context.root.uri' init parameter is not defined)"
        );
    }
    
    resolveResourceVolumes(context);
    
    
    try
    {
      initContextResourceMap();
      initContextDictionary(context);

      initLibrary();
    }
    catch (ContextualException x)
    { throw new ServletException(x);
    }
    
    if (config.getInitParameter("updateIntervalMs")!=null)
    { 
      updateIntervalMs
        =Integer.parseInt(config.getInitParameter("updateIntervalMs"));
    }

    appContextFilter.setContainer(publishRoot.asContainer());
    appContextFilter.init(config);
    
    push();
    
    try
    {
      Resource defaultCodeContext;
      try
      { 
        defaultCodeContext
          =Resolver.getInstance().resolve("context://code/");
        Package rootPackage
          =Package.fromContainer(defaultCodeContext,false);
        if (rootPackage!=null)
        { 
          defaultCodeContext
            =new OverlayResource
              (defaultCodeContext.getURI()
              ,defaultCodeContext
              ,Resolver.getInstance().resolve(rootPackage.getBase())
              );
        }
      }
      catch (UnresolvableURIException x)
      { 
        throw new ServletException
          ("Error resolving context://code/",x);
      }
      catch (ContextualException x)
      { 
        throw new ServletException
          ("Error resolving context://code/ package",x);
      }
    
      publishOverlay
        =new OverlayResource
          (publishRoot.getURI()
          ,publishRoot
          ,defaultCodeContext
          );

      if (contextDictionary.find
          ("spiralcraft.servlet.autofilter.tracePathResolution","false")
          .equals("true")
         )
      { 
        this.tracePathResolution=true;
        log.fine("Tracing path resolution for context in "+publishRoot);
        log.fine("publishOverlay="+publishOverlay);
      }
      
      updateConfig();
    }
    finally
    { pop();
    }
    
    
    // Prime various paths
    prime("/");
  }
  
  
  private void prime(String path)
    throws ServletException
  {
    InternalHttpServletRequest request
      =new InternalHttpServletRequest();
    request.setRequestURI("/");
    
    HttpServletResponse response
      =new InternalHttpServletResponse();
        
    try
    { doFilter(request,response,null);
    }
    catch (IOException x)
    { throw new ServletException("IOException while priming "+path,x);
    }
  }
  
  /**
   * <p>Resolve locations for various contextual resource volumes
   * </p>
   * 
   * @param context
   */
  private void resolveResourceVolumes(ServletContext context)
  {
    
    URI webInfRoot=publishRoot.getURI().resolve("WEB-INF/");
    
    if (instanceRootURI==null)
    {
      String instanceRoot
        =context.getInitParameter("spiralcraft.instance.rootURI");

      if (instanceRoot!=null)
      { 
        instanceRootURI
          =URIUtil.ensureTrailingSlash
            (URI.create(URLEncoder.encode(instanceRoot))
            );
      
        if (!instanceRootURI.isAbsolute())
        { instanceRootURI=publishRoot.getURI().resolve(instanceRootURI);
        }
      
      }
      else
      { instanceRootURI=webInfRoot;
      }
    }
    
    log.info("web instance root: "+instanceRootURI);
    dataURI=resolveResourceVolume
      (context,instanceRootURI,dataURI,"spiralcraft.instance.dataURI");
    log.info("context://data = "+dataURI);
    configURI=resolveResourceVolume
      (context,instanceRootURI,configURI,"spiralcraft.instance.configURI");
    log.info("context://config = "+configURI);
    filesURI=resolveResourceVolume
      (context,instanceRootURI,filesURI,"spiralcraft.instance.filesURI");
    log.info("context://files = "+filesURI);
    extURI=resolveResourceVolume
      (context,instanceRootURI,extURI,"spiralcraft.instance.extURI");
    log.info("context://ext = "+extURI);
    codeURI=resolveResourceVolume
      (context,webInfRoot,codeURI,"spiralcraft.instance.codeURI");
    log.info("context://code = "+codeURI);
    themeURI=resolveResourceVolume
      (context,webInfRoot,themeURI,"spiralcraft.instance.themeURI");
    log.info("context://theme = "+themeURI);
    
  }
  
  private URI resolveResourceVolume
    (ServletContext context
    ,URI rootURI
    ,URI defaultURI
    ,String propName
    )
  {
    String uriParam
      =context.getInitParameter(propName);
    URI ret=defaultURI;
    if (uriParam!=null)
    {
      ret=URI.create(uriParam);
      if (!ret.isOpaque())
      {
        ret
          =URIUtil.ensureTrailingSlash(ret);
      }
    }
    
    if (!ret.isAbsolute())
    { ret=rootURI.resolve(ret);
    }
    
    return ret;
    
  }
  

  
  @SuppressWarnings("unchecked")
  private void initContextDictionary(ServletContext context)
  {
    Enumeration<String> params=context.getInitParameterNames();
    while (params.hasMoreElements())
    { 
      String name=params.nextElement();
      contextDictionary.set(name, context.getInitParameter(name));
      if (debug)
      { log.fine(name+" = "+contextDictionary.find(name));
      }
    }
  }
  

  
  /**
   * Filter.doFilter() Run the filter chain appropriate for the request path
   */
  @Override
  public void doFilter
    (ServletRequest servletRequest
    ,ServletResponse servletResponse
    ,FilterChain endpoint
    )
    throws ServletException,IOException
  {
    
    push();
    try
    {
    
      updateConfig();
      
      HttpServletRequest request=(HttpServletRequest) servletRequest;
    
      if (throwable==null)
      {
        String pathString=request.getRequestURI();
        
     
        Focus<?> lastFocus=null;
        if (focus!=null)
        {
          lastFocus=FocusFilter.getFocusChain(request);
          FocusFilter.setFocusChain(request,focus);
        }
        
        try
        {
          FilterChain chain=resolveChain(pathString,endpoint);
          if (chain!=null)
          { chain.doFilter(servletRequest,servletResponse);
          }
          else if (debug)
          { log.fine("No filter chain for "+pathString);
          }
        }
        finally
        {
          if (focus!=null)
          { FocusFilter.setFocusChain(request,lastFocus);
          }
        }
        
      }
      else
      { 
        if (showExceptions)
        { sendError(servletResponse,throwable);
        }
        else
        { ((HttpServletResponse) servletResponse).sendError(500);
        }
      }
      
    }
    catch (ServletException x)
    { 
      if (showExceptions)
      { 
        sendError(servletResponse,x);
      }
      else
      { throw x;
      }

      if (x.getRootCause()!=null)
      { log.log(Level.WARNING,x.toString(),x);
      }
    }
    catch (RuntimeException x)
    { 
      if (showExceptions)
      { sendError(servletResponse,x);
      }
      else
      { throw x;
      }
      log.log(Level.WARNING,"Exception handling request",x);
    }
    finally
    { pop();
    }
    
  }
  
  

  
  /**
   * Filter.destroy()
   */
  @Override
  public void destroy()
  { 
    log.info("Shutting down WebApplicationContext");
    deleteRecursive(pathTree);
    try
    { stop();
    }
    catch (LifecycleException x)
    { log.log(Level.WARNING,"Error stopping WebApplicationContext",x);
    }
  }
  
  private synchronized void updateConfig()
  {
    if (publishOverlay==null)
    { return;
    }
    

    long time=Clock.instance().approxTimeMillis();
    if (time==0 || (updateIntervalMs>0 && time-lastUpdate>updateIntervalMs))
    { 
      throwable=null;
      // System.err.println("Controller.updateConfig(): scanning");
      try
      { updateRecursive(pathTree,publishOverlay,false);
      }
      catch (Throwable x)
      { 
        throwable=x;
        log.log(Level.WARNING,"Uncaught exception loading AutoFilters",x);
      }
      finally
      { lastUpdate=Clock.instance().approxTimeMillis();
      }
    }
  }
  
  /**
   * Called the first time a resource is resolved from a VFS container
   *
   * Ensures that the traversal of the tree properly tracks package grafts for
   *   each path segment
   * 
   * @param resource
   * @return
   * @throws IOException
   */
  // TODO: This whole operation should be opaque and moved to spiralcraft.vfs.
  //       There is a well defined model for how the filesystem overlays work
  //          with respect to package grafts.
  //       One way this can be handled more formally is to have a PackageResource
  //          that handles the grafts internally.
  private Resource virtualize(Resource resource) 
    throws IOException,ContextualException
  {
    if (resource.asContainer()!=null)
    {
      Resource top = 
        (resource instanceof OverlayResource)
        ?((OverlayResource) resource).getOverlay()
        :resource;
      Package pkg=Package.fromThisContainer(top);
      if (pkg!=null)
      {
        // The local copy contains a package graft. Replace any existing package
        //   graft with the specified graft.
        if (debug || tracePathResolution)
        { log.fine("Controller.virtualize(): "+resource+" rebasing to "+pkg.getBase());
        }
        resource=new OverlayResource
          (resource.getURI()
          ,top
          ,Resolver.getInstance().resolve(pkg.getBase())
          );
        return resource;
      }
      
      if (resource instanceof OverlayResource)
      { 
        pkg=Package.fromThisContainer(((OverlayResource) resource).getBase());
        if (pkg!=null)
        {
          // The base package stack contains a package graft. Make sure it's picked
          //   up.
          if (debug || tracePathResolution)
          { log.fine("Controller.virtualize(): "+resource+" adding base underlay "+pkg.getBase());
          }
          resource=new OverlayResource
            (resource.getURI()
            ,resource
            ,Resolver.getInstance().resolve(pkg.getBase())
            );
        }
        return resource;
      
      }
    }
    
    if (resource instanceof OverlayResource)
    { 
      // We're already virtualized
      return resource;
    }
    
    // See if this resource maps to a base tree defined by a package in a
    //   parent container.
    Resource baseResource=Package.findBaseResource(resource);
    if (baseResource!=null)
    {
      // Make sure we're virtualized properly
      if (debug || tracePathResolution)
      { log.fine("Controller.virtualize(): "+resource.getURI()+" mapping to existing base tree "+baseResource.getURI());
      }

      resource=new OverlayResource
        (resource.getURI()
        ,resource
        ,baseResource
        );
    }
    return resource;
  }
  
  private void updateRecursive
    (PathTree<FilterSet> node
    ,Resource resource
    ,boolean dirty
    )
    throws IOException,ContextualException
  {
    if (debug || tracePathResolution)
    { log.fine("Controller.updateRecursive(): processing "+resource.getURI()+" for "+node.getPath());
    }
    if (!resource.exists())
    { 
      if (tracePathResolution)
      { log.fine("Deleting "+node.getPath());
      }
      deleteRecursive(node);
    }
    else
    {
      FilterSet filterSet=node.get();
      if (filterSet!=null)
      { 
        if (filterSet.checkDirtyResource())
        { 
          if (tracePathResolution)
          { log.fine("Dirty "+node.getPath());
          }
          dirty=true;
          filterSet.clear();
          node.set(null);
          filterSet=null;
        }
      }
    
      if (filterSet==null)
      {
        filterSet=new FilterSet(resource,node,config);
        if (!filterSet.isNull())
        { dirty=true;
        }
        else
        { filterSet=null;
        }
      }
      
      if (dirty && filterSet!=null)
      { filterSet.compute();
      }
      
      if (dirty && uriCache.size()>0)
      { uriCache.clear();
      }
      
      if (filterSet!=null && filterSet.getException()!=null)
      { 
        log.fine("Aborting recursion");
        // Don't bother with sub-filters if there was a problem here
        return;
      }
      
        // Handle the existing children
      for (PathTree<FilterSet> child
          : node.getChildren()
          )
      { 
        updateRecursive
          (child
          ,child.get()!=null && child.get().getContainerResource()!=null
            ?child.get().getContainerResource()
            :resource.asContainer().getChild(child.getName())
          ,dirty
          );
      }

      if (debug)
      { log.fine("Checking for new children in "+resource);
      }
      
      // Handle any new children
      for (Resource childResource: resource.asContainer().listChildren(exclusionFilter))
      { 
        childResource=virtualize(childResource);
        if (tracePathResolution)
        { log.fine(node.getPath()+"->"+childResource.getLocalName()+" --> "+childResource.toString());
        }
        if (childResource.asContainer()!=null
            && node.getChild(childResource.getLocalName())==null
            )
        { 
          PathTree<FilterSet> childNode
            =new PathTree<FilterSet>(childResource.getLocalName());
          node.addChild(childNode);
          if (debug)
          { log.fine("Added "+childResource);
          }
          updateRecursive(childNode,childResource,dirty);
        }
      }
      
      // Handle any grafts onto web tree
      Graft[] grafts=codeAuthority.getGrafts(node.getPath().toString().substring(1));
      if (grafts!=null) 
      {
        for (Graft graft: grafts)
        { 
          Resource childResource=virtualize(graft.resolve(URI.create("")));
          String localName=new Path(graft.getVirtualURI().getPath()).lastElement();
          if (childResource.asContainer()!=null
              && node.getChild(localName)==null
              )
          { 
            PathTree<FilterSet> childNode
              =new PathTree<FilterSet>(localName);
            node.addChild(childNode);
            if (debug)
            { log.fine("Added "+childResource);
            }
            updateRecursive(childNode,childResource,dirty);
          }
        } 
        
      }
      
      
    }
  }
  
  /**
   * Prune a branch off the pathTree that corresponds to a
   *   deleted filesystem tree
   */
  private void deleteRecursive(PathTree<FilterSet> node)
  {
    FilterSet filterSet=node.get();
    if (filterSet!=null)
    { 
      node.set(null);
    }
    if (node.getParent()!=null)
    { node.getParent().removeChild(node);
    }
    
    LinkedList<PathTree<FilterSet>> deleteList
      =new LinkedList<PathTree<FilterSet>>();
    
    for (PathTree<FilterSet> child: node)
    { deleteList.add(child);
    }
    
    for (PathTree<FilterSet> child: deleteList)
    { deleteRecursive(child);
    }
    
    if (filterSet!=null)
    { 
      filterSet.clear();
      filterSet=null;
    }
    
  }
  
  /**
   * @return A FilterChain from the cache, or a newly created FilterChain
   */
  private synchronized FilterChain resolveChain
    (String pathString
    ,FilterChain endpoint
    )
  {
    FilterChain chain=findCachedChain(pathString,endpoint);
    if (chain==null)
    { 
      chain=createChain(pathString,endpoint);
      CacheEntry entry=new CacheEntry();
      entry.chain=chain;
      entry.endpoint=endpoint;
      entry.pathString=pathString;
      uriCache.put(pathString, entry);
    }
    return chain;
      
  }
  
  /**
   * @return A new, uncached chain for the specified path
   */
  private FilterChain createChain
    (String pathString,FilterChain endpoint)
  {
    Path path=new Path(pathString,'/');
    PathTree<FilterSet> pathTree=this.pathTree.findDeepestChild(path);
    if (tracePathResolution)
    { log.fine("Deepest path for "+pathString+" is "+pathTree.getPath());
    }
    
    FilterSet filterSet=pathTree.get();
    while (filterSet==null)
    {
      pathTree=pathTree.getParent();
      if (pathTree==null)
      { break;
      }
      filterSet=pathTree.get();
    }
    
    if (filterSet==null)
    { 
      // No filters anywhere
      return endpoint;
    }

    LinkedFilterChain first=new LinkedFilterChain(appContextFilter);
    LinkedFilterChain last=first;
    
    for (AutoFilter autoFilter: filterSet.getEffectiveFilters())
    {
      if (autoFilter.appliesToPath(path))
      {
        LinkedFilterChain next=last;
        last=new LinkedFilterChain(autoFilter);
        next.setNext(last);
      }
    }
    
    last.setNext(endpoint);
    return first;
    
  }
  
  public void sendError(ServletResponse servletResponse,Throwable x)
    throws IOException
  {
    HttpServletResponse response=(HttpServletResponse) servletResponse;
    response.setStatus(500);

    PrintWriter printWriter=new PrintWriter(response.getWriter());
    printWriter.write(x.toString()+"\r\n");

    x.printStackTrace(printWriter);
    printWriter.flush();
  }
  
  /**
   * 
   * @return An FilterChain from the cache that matches the path and the specified endpoint
   */
  private FilterChain
    findCachedChain(String pathString,FilterChain endpoint)
  {
    CacheEntry entry=uriCache.get(pathString);
    if (entry==null)
    { return null;
    }
    if (entry.endpoint!=endpoint)
    { 
      // Container has changed its servlet mapping
      uriCache.remove(pathString);
      return null;
    }
    return entry.chain;
  }
  
  class CacheEntry
  {
    String pathString;
    FilterChain endpoint;
    FilterChain chain;
  }
  



}
