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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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

import spiralcraft.data.persist.PersistenceException;
import spiralcraft.data.persist.XmlBean;
import spiralcraft.log.ClassLog;
import spiralcraft.servlet.util.LinkedFilterChain;
import spiralcraft.time.Clock;
import spiralcraft.time.Scheduler;
import spiralcraft.util.ContextDictionary;
import spiralcraft.util.Path;
import spiralcraft.util.tree.PathTree;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.context.ContextResourceMap;
import spiralcraft.vfs.file.FileResource;




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
  implements Filter
{
  private static final ClassLog log=ClassLog.getInstance(Controller.class);
  
  private int updateIntervalMs=10000;
  private String controlFileName=".control.xml";
  
  private final HashMap<String,CacheEntry> uriCache
    =new HashMap<String,CacheEntry>();
  
  private final PathTree<FilterSet> pathTree
    =new PathTree<FilterSet>(null);
  
  private FilterConfig config;
  private Resource root;
  private long lastUpdate;
  private final ContextResourceMap contextResourceMap
    = new ContextResourceMap();
  
  private Throwable throwable;
  
  private boolean debug=false;
  
  private final ContextDictionary contextDictionary
    =new ContextDictionary(ContextDictionary.getInstance());
  
  private URI dataURI=URI.create("WEB-INF/data/");
  private URI configURI=URI.create("WEB-INF/config/");
  private URI filesURI=URI.create("WEB-INF/files/");
  private URI codeURI=URI.create("");
  
  private Scheduler scheduler;
  
  

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
  { this.dataURI=dataURI;
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
  { this.configURI=configURI;
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
  { this.filesURI=filesURI;
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
  { this.codeURI=codeURI;
  }
  
  /**
   * Filter.init()
   */
  public void init(FilterConfig config)
    throws ServletException
  {
    this.config=config;
    ServletContext context=config.getServletContext();
    
    String realPath=context.getRealPath("/");
    if (realPath!=null)
    { root=new FileResource(new File(realPath));
    }
    if (debug)
    { log.fine("Root is "+root.getURI());
    }
    
    initContextResourceMap(context);
    initContextDictionary(context);
    
    if (config.getInitParameter("updateIntervalMs")!=null)
    { 
      updateIntervalMs
        =Integer.parseInt(config.getInitParameter("updateIntervalMs"));
    }
    
    push();
    try
    { updateConfig();
    }
    finally
    { pop();
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
    URI contextURI=null;
    try
    { 
      contextURI = context.getResource("/").toURI();
      if (debug)
      { log.fine("Context is "+contextURI);
      }
    }
    catch (URISyntaxException x)
    { 
      try
      {
        throw new ServletException
          ("Error reading context URL '"
          +context.getResource("/").toString()
          ,x);
      }
      catch (MalformedURLException y)
      { y.printStackTrace();
      }
    }
    catch (MalformedURLException x)
    { x.printStackTrace();
    }
//    System.err.println
//      ("Controller.init(): path="+realPath+" contextURI="+contextURI);
    
    // Bind the "context://www","context://data" resources to this thread.
    contextResourceMap.put("war",contextURI);
    contextResourceMap.putDefault(contextURI);
    contextResourceMap.put("data",contextURI.resolve(dataURI));
    contextResourceMap.put("config",contextURI.resolve(configURI));
    contextResourceMap.put("files",contextURI.resolve(filesURI));
    contextResourceMap.put("code",contextURI.resolve(codeURI));
    
  }
  
  /**
   * Filter.doFilter() Run the filter chain appropriate for the request path
   */
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
        FilterChain chain=resolveChain(pathString,endpoint);
        chain.doFilter(servletRequest,servletResponse);
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
  }
  
  protected void pop()
  {
    Scheduler.pop();
    contextResourceMap.pop();
    ContextDictionary.popInstance();
  }
  
  /**
   * Filter.destroy()
   */
  public void destroy()
  { deleteRecursive(pathTree);
  }
  
  private synchronized void updateConfig()
  {
    if (root==null)
    { return;
    }
    
    long time=Clock.instance().approxTimeMillis();
    if (time==0 || (updateIntervalMs>0 && time-lastUpdate>updateIntervalMs))
    { 
      throwable=null;
      // System.err.println("Controller.updateConfig(): scanning");
      try
      { updateRecursive(pathTree,root,false);
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
        Resource controlResource
          =resource.asContainer().getChild(controlFileName);
        if (controlResource.exists())
        { 
//          System.err.println
//            ("Controller.updateRecursive(): Found "
//            +controlResource.getURI()+"!"
//            );
              
          dirty=true;
          filterSet=new FilterSet();
          try
          { 
            filterSet.loadResource(controlResource,node.getPath());
            node.set(filterSet);
            filterSet.node=node;
            
            Resource errorResource=resource.asContainer().getChild(controlFileName+".err");
            try
            {
              if (filterSet.exception==null)
              {
                if (errorResource.exists())
                { errorResource.delete();
                }
              }
              else
              { 
                OutputStream out=errorResource.getOutputStream();
                PrintStream pout=new PrintStream(out);
                pout.println("Error processing filter definitions");
                pout.println(filterSet.exception.toString());
                pout.println("Stack trace ----------------------------------------");
                filterSet.exception.printStackTrace(pout);
                pout.flush();
                out.flush();
                out.close();
              }
            }
            catch (IOException y)
            { System.err.println("Controller: error writing contol file success: "+y);
            }
            
          }
          catch (Throwable x)
          { 
            
            Resource errorResource=resource.asContainer().getChild(controlFileName+".err");
            try
            {
              PrintStream out=new PrintStream(errorResource.getOutputStream());
              out.println("Uncaught error processing filter definitions");
              out.println(x.toString());
              out.println("Stack trace ----------------------------------------");
              x.printStackTrace(out);
              out.flush();
              out.close();
            }
            catch (IOException y)
            { System.err.println("Controller: error writing contol file error: "+y);
            }
            
            filterSet.loadError(node.getPath(), x);
            
          }
        }
//        else
//        { System.err.println("Controller.updateRecursive(): No "+controlResource.getURI());
//        }
      }
      
      if (dirty && filterSet!=null)
      { filterSet.compute();
      }
      
      if (dirty && uriCache.size()>0)
      { uriCache.clear();
      }
      
      if (filterSet!=null && filterSet.exception!=null)
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
    for (PathTree<FilterSet> child: node)
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

    LinkedFilterChain first=null;
    LinkedFilterChain last=null;
    
    for (AutoFilter autoFilter: filterSet.effectiveFilters)
    {
      if (autoFilter.appliesToPath(path))
      {
        if (first==null)
        { 
          first=new LinkedFilterChain(autoFilter);
          last=first;
        }
        else
        { 
          LinkedFilterChain next=last;
          last=new LinkedFilterChain(autoFilter);
          next.setNext(last);
        }
      }
    }
    
    if (last!=null)
    { 
      last.setNext(endpoint);
      return first;
    }
    else
    { return endpoint;
    }
    
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
  
  /**
   * All the Filters configured for a resource node (directory)
   */
  class FilterSet
  {
    final ArrayList<AutoFilter> effectiveFilters
      =new ArrayList<AutoFilter>();
    
    final ArrayList<AutoFilter> localFilters
      =new ArrayList<AutoFilter>();
    
    Resource resource;
    long lastModified;
    Throwable exception;

    PathTree<FilterSet> node;
    
    /**
     * Check to see if a Resource has been modified
     */
    public boolean checkDirtyResource()
    {
      
      if (resource!=null)
      {
        try
        {
          if (!resource.exists())
          { return true;
          }
          else if (resource.getLastModified()!=lastModified)
          { return true;
          }
        }
        catch (IOException x)
        { return true;
        }
      }
      return false;
    }
    
    
    /**
     * Called when a resource is detected as being removed or changed
     */
    public void clear()
    { 
      for (AutoFilter filter: localFilters)
      { filter.destroy();
      }
      exception=null;
      resource=null;
      localFilters.clear();
      effectiveFilters.clear();
      lastModified=0;
    }
    
    private boolean patternsIntersect(String pattern1, String pattern2)
    {
      if (pattern1.equals("*"))
      { return true;
      }
      else if (pattern2.equals("*"))
      { return true;
      }
      else if (pattern1.equals(pattern2))
      { return true;
      }
      return false;
    }
    
    /**
     * Called to recompute the effective filters
     */
    public void compute()
    { 
//      System.err.println("Controller.FilterSet.compute()");
      effectiveFilters.clear();
      
      ArrayList<AutoFilter> localExcludes
        =new ArrayList<AutoFilter>();
      
      
      FilterSet parentSet=findParentSet();
      if (parentSet!=null)
      {
        // Integrate the parent filter set into this level
        for (AutoFilter parentFilter: parentSet.effectiveFilters)
        { 
          if (parentFilter.isGlobal())
          { 
            // Only global filters are considered for inheritance
            
            boolean addParent=true;
            for (AutoFilter localFilter : localFilters)
            {
              if (localFilter.getCommonType()
                  .isAssignableFrom(parentFilter.getClass())
                  )
              {
                
                if (patternsIntersect
                      (parentFilter.getPattern(),localFilter.getPattern()))
                {
                
                  // Determine how the parent filter relates to a local filter
                  //   of a compatible type and an intersecting pattern
                
                  if (parentFilter.isOverridable())
                  {
                    if (!localFilter.isAdditive())
                    { 
                      // Completely override the parent
                      addParent=false;
                    }

                    // Let the filter and it's parent figure out what to do
                    localFilter.setGeneralInstance(parentFilter);
                  }
                  else
                  { 
                    if (!localFilter.isAdditive())
                    { 
                      // Completely ignore the local filter
                      localExcludes.add(localFilter);
                    }
                    else
                    { 
                      // Let the filter and it's parent figure out what to do
                      localFilter.setGeneralInstance(parentFilter);
                    }
                  }
                }
              }
            }
            
            if (addParent)
            { effectiveFilters.add(parentFilter);
            }
          }
          
        }
      }
      
      for (AutoFilter filter: localFilters)
      { 
        if (!localExcludes.contains(filter))
        { effectiveFilters.add(filter);
        }
      }
    }
    
    private FilterSet findParentSet()
    { 
      // Called from compute()
      PathTree<FilterSet> parentNode=node.getParent();
      FilterSet parentSet=null;
      while (parentNode!=null && parentSet==null)
      {
        parentSet=parentNode.get();
        parentNode=parentNode.getParent();
      }
      return parentSet;
    }
    
    public void loadResource(Resource resource,Path container)
    { 
      exception=null;
      try
      {
        this.resource=resource;
        this.lastModified=resource.getLastModified();
      
        // XXX Set a context so that resources can access the web root
        //   file system in a context independent manner
      
        XmlBean <List<AutoFilter>> bean
          =new XmlBean<List<AutoFilter>>
            (URI.create("class:/spiralcraft/servlet/autofilter/AutoFilter.list")
            ,resource.getURI()
            );
      
        List<AutoFilter> filters=bean.get();
        localFilters.addAll(filters);
      
        for (AutoFilter filter: localFilters)
        { 
          filter.setPath(container);
          // TODO: Make a Focus chain
          //   filter.bind(lastFilter.getFocus());
          //   
          filter.init(config);
        }
      }
      catch (PersistenceException x)
      { 
        log.log(Level.WARNING,"Controller.loadResource() failed",x);
        loadError(container,x);
      }
      catch (ServletException x)
      { 
        log.log(Level.WARNING,"Controller.loadResource() failed",x);
        loadError(container,x);
      }
      catch (IOException x)
      { 
        log.log(Level.WARNING,"Controller.loadResource() failed",x);
        loadError(container,x);
      }
      catch (RuntimeException x)
      { 
        log.log(Level.WARNING,"Controller.loadResource() failed",x);
        throw x;
      }

    }

    public void loadError(Path container,Throwable x)
    {
      ErrorFilter filter=new ErrorFilter();
      exception=x;
      filter.setThrowable(x);
      filter.setPath(container);
      try
      { filter.init(config);
      }
      catch (ServletException y)
      { 
        // Never happens
        y.printStackTrace();
        x.printStackTrace();
      }
      localFilters.add(filter);
    }
  }

}
