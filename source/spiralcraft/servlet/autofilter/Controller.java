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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

import spiralcraft.bundle.Bundle;
import spiralcraft.bundle.BundleClassLoader;
import spiralcraft.bundle.Library;
import spiralcraft.lang.BindException;
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

import spiralcraft.log.ClassLog;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.servlet.kit.StandardFilterConfig;
import spiralcraft.servlet.util.LinkedFilterChain;
import spiralcraft.text.html.URLEncoder;
import spiralcraft.time.Clock;
import spiralcraft.time.Scheduler;
import spiralcraft.util.ContextDictionary;
import spiralcraft.util.Path;
import spiralcraft.util.URIUtil;
import spiralcraft.util.tree.PathTree;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;
import spiralcraft.vfs.context.Authority;
import spiralcraft.vfs.context.ContextResourceMap;
import spiralcraft.vfs.context.Redirect;
import spiralcraft.vfs.file.FileResource;
import spiralcraft.vfs.ovl.OverlayResource;




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
  implements Filter,Contextual
{
  private static final ClassLog log=ClassLog.getInstance(Controller.class);
  
  private int updateIntervalMs=10000;
  
  private final HashMap<String,CacheEntry> uriCache
    =new HashMap<String,CacheEntry>();
  
  private final PathTree<FilterSet> pathTree
    =new PathTree<FilterSet>(null);
  
  private FilterConfig config;
  private Resource publishRoot;
  private Resource publishOverlay;
  private long lastUpdate;
  private final ContextResourceMap contextResourceMap
    = new ContextResourceMap();
  
  private Throwable throwable;
  
  private boolean debug=false;
  
  private final ContextDictionary contextDictionary
    =new ContextDictionary
      (ContextDictionary.getInstance()
      ,new HashMap<String,String>()
      ,true
      );
  
  private URI dataURI=URI.create("data/");
  private URI configURI=URI.create("config/");
  private URI filesURI=URI.create("files/");
  private URI codeURI=URI.create("webui/");
  private URI themeURI=URI.create("webui/theme/");
  
  private HashMap<String,Path> bundleMountPointMap
    =new HashMap<String,Path>();
  {
    bundleMountPointMap.put("war-js",Path.create("js"));
    bundleMountPointMap.put("war-css",Path.create("css"));
    bundleMountPointMap.put("war-public",Path.create(""));
    bundleMountPointMap.put("war-public-images",Path.create("images"));
  }
  
  private BundleClassLoader bundleClassLoader;
  private ThreadLocal<ClassLoader> oldContextClassLoader
    =new ThreadLocal<ClassLoader>();
  
  private Authority rootAuthority;
  
  private Scheduler scheduler;

  private Focus<?> focus;  
  
  private AppContextFilter appContextFilter
    =new AppContextFilter();
  { 
    appContextFilter.setGlobal(true);
    appContextFilter.setPattern("*");
  }
  

  /**
   * <p>The root URI where modifiable persistent data is kept. This is
   *   normally replicated at a higher level than the filesystem
   * </p>
   *
   * <p>This is resolvable via the "context://data/" URI
   * </p>
   *
   * 
   * <p>If a relative URI is specified, it will be relative to the context
   *   root.
   * </p>
   * 
   * <p>defaults to WEB-INF/data/
   * </p>
   * 
   * @param dataURI
   */
  public void setDataURI(URI dataURI)
  { this.dataURI=cleanURI(dataURI);
  }
      
  /**
   * <p>The root URI where application configuration artifacts are kept. This
   *   is normally non-writable private data that has a relationship to the
   *   deployment.
   * </p>
   * 
   * <p>If a relative URI is specified, it will be relative to the context
   *   root.
   * </p>
   * 
   * <p>This is resolvable via the "context://config/" URI
   * </p>
   *
   * <p>defaults to WEB-INF/config/
   * </p>
   * 
   * @param dataURI
   */
  public void setConfigURI(URI configURI)
  { this.configURI=cleanURI(configURI);
  }
      
  /**
   * <p>The root URI of the main directory for dynamic file storage. This
   *   is where data replication is handled by VFS. 
   * </p>
   * 
   * <p>If a relative URI is specified, it will be relative to the context
   *   root.
   * </p>
   * 
   * <p>This is resolvable via the "context://files/" URI
   * </p>
   *
   * <p>defaults to WEB-INF/files/
   * </p>
   * 
   * @param dataURI
   */
  public void setFilesURI(URI filesURI)
  { this.filesURI=cleanURI(filesURI);
  }
  
  /**
   * <p>The root URI for the context://code/ authority where server executable
   *   code artifacts can be found. It is usually a non-public "look-aside"
   *   tree that can map functionality into a context.
   * </p>
   * 
   * @param codeURI
   */
  public void setCodeURI(URI codeURI)
  { this.codeURI=cleanURI(codeURI);
  }
  
  /**
   * <p>The URI for the context://theme authority used to locate components
   *   in extensible themes.
   * </p>
   * 
   * @param codeURI
   */
  public void setThemeURI(URI themeURI)
  { this.themeURI=cleanURI(themeURI);
  }
  
  public void setDebug(boolean debug)
  { this.debug=debug;
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
    
    
    initContextResourceMap(context);
    initContextDictionary(context);
    
    initLibrary();
    
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
      }
      catch (UnresolvableURIException x)
      { 
        throw new ServletException
          ("Error resolving context://code/",x);
      }
    
      publishOverlay
        =new OverlayResource
          (publishRoot.getURI()
          ,publishRoot
          ,defaultCodeContext
          );

      updateConfig();
      
    }
    finally
    { pop();
    }
    
  }
  
  
  private void initLibrary()
    throws ServletException
  {
    try
    {
      Resource packagesResource
        =Resolver.getInstance()
          .resolve(publishRoot.getURI().resolve("WEB-INF/packages"));
    
      Container packagesContainer
        =packagesResource.asContainer();
    
      if (packagesContainer!=null)
      { Library.set(new Library(packagesContainer));
      }
      
      // TODO: We need make loading system bundles a context parameter, and/or
      //   express isolation as part of the package library configuration
      Bundle[] bundles
        =Library.get().getAllBundles();
      
      ArrayList<String> classBundles=new ArrayList<String>();
      ArrayList<String> jarLibBundles=new ArrayList<String>();
      
      for (Bundle bundle: bundles)
      { 
        Path mountPoint=mountPointForBundle(bundle);
        if (mountPoint!=null)
        {
          rootAuthority.mapPath
            (mountPoint.toString()
            ,new Redirect
              (URI.create(mountPoint.toString()),bundle.getBundleURI())
            );
          log.fine("Mounted "+bundle.getBundleURI()+" to "+mountPoint);
        }
        
        if (bundle.getBundleName().equals("war-classes"))
        { classBundles.add(bundle.getAuthorityName());
        }
        else if (bundle.getBundleName().equals("war-lib"))
        { jarLibBundles.add(bundle.getAuthorityName());
        }
      }
      
      bundleClassLoader
        =new BundleClassLoader
          (classBundles.toArray(new String[classBundles.size()])
          ,jarLibBundles.toArray(new String[jarLibBundles.size()])
          );
     
    }
    catch (IOException x)
    { throw new ServletException(x);
    } 
  }
  
  private Path mountPointForBundle(Bundle bundle)
  {
    Path root=bundleMountPointMap.get(bundle.getBundleName());
    if (root==null)
    { return null;
    }
    else
    { return root.append(bundle.getPackage().getName()).asContainer();
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
    
    String instanceRoot
      =context.getInitParameter("spiralcraft.instance.rootURI");

    URI instanceRootURI;
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
    
    dataURI=resolveResourceVolume
      (context,instanceRootURI,dataURI,"spiralcraft.instance.dataURI");
    configURI=resolveResourceVolume
      (context,instanceRootURI,configURI,"spiralcraft.instance.configURI");
    filesURI=resolveResourceVolume
      (context,instanceRootURI,filesURI,"spiralcraft.instance.filesURI");
    codeURI=resolveResourceVolume
      (context,webInfRoot,codeURI,"spiralcraft.instance.codeURI");
    themeURI=resolveResourceVolume
      (context,webInfRoot,themeURI,"spiralcraft.instance.themeURI");
    
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
  

  private URI cleanURI(URI uri)
  { 
    if (!uri.getPath().endsWith("/"))
    { return URIUtil.ensureTrailingSlash(uri);
    }
    else
    { return uri;
    }
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
  
  private void initContextResourceMap(ServletContext context)
    throws ServletException
  {
    URI contextURI=publishRoot.getURI();
    rootAuthority=new Authority("",contextURI);
//    System.err.println
//      ("Controller.init(): path="+realPath+" contextURI="+contextURI);
    
    // Bind the "context://www","context://data" resources to this thread.
    contextResourceMap.put("war",contextURI);
    contextResourceMap.put(rootAuthority);
    contextResourceMap.put("data",contextURI.resolve(dataURI));
    contextResourceMap.put("config",contextURI.resolve(configURI));
    contextResourceMap.put("files",contextURI.resolve(filesURI));
    contextResourceMap.put("code",contextURI.resolve(codeURI));
    contextResourceMap.put("theme",contextURI.resolve(themeURI));
    
    if (debug)
    {
      log.debug("dataURI="+dataURI);
      log.debug("configURI="+configURI);
      log.debug("filesURI="+filesURI);
      log.debug("codeURI="+codeURI);
      log.debug("themeURI="+themeURI);
      for (String mapping:new String[]{"war","data","config","files","code","theme"})
      { log.debug("Mapped "+mapping+" to "+contextResourceMap.get(mapping));
      }
    }
    
        
    // contextResourceMap.setIsolate(true)
    try
    { contextResourceMap.bind(focus);
    }
    catch (Exception x)
    { 
      throw new ServletException
        ("Error binding Controller in "
        +contextURI.toString()
        ,x
        );
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
          chain.doFilter(servletRequest,servletResponse);
        }
        finally
        {
          if (focus!=null)
          { FocusFilter.setFocusChain(request,lastFocus);
          }
        }
        
      }
      else
      { sendError(servletResponse,throwable);
      }
      
    }
    catch (ServletException x)
    { 
      sendError(servletResponse,x);

      if (x.getRootCause()!=null)
      { log.log(Level.WARNING,x.toString(),x);
      }
    }
    catch (RuntimeException x)
    { 
      sendError(servletResponse,x);
      log.log(Level.WARNING,"Exception handling request",x);
    }
    finally
    { pop();
    }
    
  }
  
  
  protected void push()
  {
    ContextDictionary.pushInstance(contextDictionary);
    contextResourceMap.push();
    if (scheduler==null)
    { scheduler=new Scheduler();
    }
    Scheduler.push(scheduler);
    if (bundleClassLoader!=null)
    {
      oldContextClassLoader.set(Thread.currentThread().getContextClassLoader());
      Thread.currentThread().setContextClassLoader(bundleClassLoader);
    }
  }
  
  protected void pop()
  {
    if (bundleClassLoader!=null)
    { 
      Thread.currentThread().setContextClassLoader(oldContextClassLoader.get());
      oldContextClassLoader.remove();
    }
    Scheduler.pop();
    contextResourceMap.pop();
    ContextDictionary.popInstance();
  }
  
  /**
   * Filter.destroy()
   */
  @Override
  public void destroy()
  { deleteRecursive(pathTree);
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
  
  private void updateRecursive
    (PathTree<FilterSet> node
    ,Resource resource
    ,boolean dirty
    )
    throws IOException
  {
    // System.err.println("Controller.updateRecursive(): checking "+resource.getURI());
    if (!resource.exists())
    { deleteRecursive(node);
    }
    else
    {
      FilterSet filterSet=node.get();
      if (filterSet!=null)
      { 
        if (filterSet.checkDirtyResource())
        { 
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
          : node
          )
      { 
        updateRecursive
          (child
          ,resource.asContainer().getChild(child.getName())
          ,dirty
          );
      }

      
      
      // Handle any new children
      for (Resource childResource: resource.asContainer().listChildren())
      { 
        if (childResource.asContainer()!=null
            && node.getChild(childResource.getLocalName())==null
            )
        { 
          PathTree<FilterSet> childNode
            =new PathTree<FilterSet>(childResource.getLocalName());
          node.addChild(childNode);
          updateRecursive(childNode,childResource,dirty);
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
      filterSet.clear();
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
    // System.err.println("Controller.createChain() pathString="+pathString);
    
    Path path=new Path(pathString,'/');
    PathTree<FilterSet> pathTree=this.pathTree.findDeepestChild(path);
    
    // System.err.println("Controller.createChain() deepestChild= "+pathTree.getName());
    
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
      // System.err.println("Controller.createChain() No filters");
      
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
  

  @Override
  public Focus<?> bind(Focus<?> focusChain)
    throws BindException
  { 
    focus=focusChain;
    return focusChain;
  }

}
