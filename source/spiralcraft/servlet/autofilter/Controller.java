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

import spiralcraft.servlet.util.LinkedFilterChain;

import spiralcraft.util.Path;

import spiralcraft.util.tree.PathTree;

import spiralcraft.stream.Resource;

import spiralcraft.stream.file.FileResource;

import spiralcraft.stream.context.ContextResource;

import spiralcraft.time.Clock;

import spiralcraft.data.persist.XmlBean;
import spiralcraft.data.persist.PersistenceException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;




/**
 * 
 * <P>Manages Filter chains for an HTTP resource tree using definition
 *   files contained inside the resource tree. 
 * 
 * <P>One Controller is registered as a 'global' Filter for a given
 *   container context. The controller monitors the resource tree being
 *   served by the context and maintains sets of filters for specific
 *   paths as defined by the ".control.xml" control files found in the
 *   paths.
 *   
 * <P>The Controller will re-scan the resource tree for new or changed
 *   control files at a configurable interval that defaults to every 10
 *   seconds.
 */
public class Controller
  implements Filter
{
  private long updateIntervalMs=10000;
  private String controlFileName=".control.xml";
  
  private final HashMap<String,CacheEntry> uriCache
    =new HashMap<String,CacheEntry>();
  
  private final PathTree<FilterSet> pathTree
    =new PathTree<FilterSet>(null);
  
  private FilterConfig config;
  private Resource root;
  private long lastUpdate;
  private URI contextURI;

  /**
   * Filter.init()
   */
  public void init(FilterConfig config)
    throws ServletException
  {
    this.config=config;
    String realPath=config.getServletContext().getRealPath("/");
    if (realPath!=null)
    { root=new FileResource(new File(realPath));
    }
    
    try
    { contextURI =config.getServletContext().getResource("/").toURI();
    }
    catch (URISyntaxException x)
    { 
      try
      {
        throw new ServletException
          ("Error reading context URL '"
          +config.getServletContext().getResource("/").toString()
          ,x);
      }
      catch (MalformedURLException y)
      { y.printStackTrace();
      }
    }
    catch (MalformedURLException x)
    { x.printStackTrace();
    }
    System.err.println
      ("Controller.init(): path="+realPath+" contextURI="+contextURI);
    
    // Bind the "context://war" resource prefix to this thread.
    URI oldWar=ContextResource.lookup("war");
    ContextResource.bind("war",contextURI);
    try
    { updateConfig();
    }
    finally
    { ContextResource.bind("war",oldWar);
    }
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
    // Bind the "context://war" resource prefix to this thread.
    URI oldWar=ContextResource.lookup("war");
    ContextResource.bind("war",contextURI);
    try
    {
    
      updateConfig();
      HttpServletRequest request=(HttpServletRequest) servletRequest;
    
      String pathString=request.getRequestURI();
      FilterChain chain=resolveChain(pathString,endpoint);
      chain.doFilter(servletRequest,servletResponse);
      
    }
    finally
    { ContextResource.bind("war",oldWar);
    }
    
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
    if (time-lastUpdate>updateIntervalMs)
    { 
      // System.err.println("Controller.updateConfig(): scanning");
      try
      { updateRecursive(pathTree,root,false);
      }
      catch (IOException x)
      { x.printStackTrace();
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
          System.err.println("Controller.updateRecursive(): Found "+controlResource.getURI()+"!");
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
              OutputStream out=errorResource.getOutputStream();
              out.write("Successfully processed control file".getBytes());
              out.flush();
              out.close();
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
              out.println("Error processing filter definitions");
              out.println(x.toString());
              out.println("Stack trace ----------------------------------------");
              x.printStackTrace(out);
              out.flush();
              out.close();
            }
            catch (IOException y)
            { System.err.println("Controller: error writing contol file error: "+y);
            }
            
            filterSet=null;
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
      System.err.println("Controller.FilterSet.compute()");
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
                    localFilter.setParentInstance(parentFilter);
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
                      localFilter.setParentInstance(parentFilter);
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
      throws IOException,PersistenceException
    { 
      this.resource=resource;
      this.lastModified=resource.getLastModified();
      
      // XXX Set a context so that resources can access the web root
      //   file system in a context independent manner
      
      XmlBean <List<AutoFilter>> bean
        =new XmlBean<List<AutoFilter>>
          (URI.create("java:/spiralcraft/servlet/autofilter/AutoFilter.list")
          ,resource.getURI()
          );
      
      List<AutoFilter> filters=bean.get();
      localFilters.addAll(filters);
      
      for (AutoFilter filter: localFilters)
      { 
        filter.setPath(container);
        filter.init(config);
      }

    }
  }
  
}
