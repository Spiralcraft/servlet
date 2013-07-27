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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import spiralcraft.data.persist.PersistenceException;
import spiralcraft.data.persist.XmlBean;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.servlet.kit.StandardFilterConfig;
import spiralcraft.util.Path;
import spiralcraft.util.tree.PathTree;
import spiralcraft.vfs.Resource;

/**
 * All the Filters configured for a resource node (directory)
 */
class FilterSet
{

  private static final ClassLog log=ClassLog.getInstance(FilterSet.class);
  private FilterConfig config;
  
  private String controlFileName=".control.xml";

  private final ArrayList<AutoFilter> effectiveFilters
    =new ArrayList<AutoFilter>();
  
  private final ArrayList<AutoFilter> localFilters
    =new ArrayList<AutoFilter>();
  
  private AutoFilter pathContextFilter
    =new PathContextFilter();
  
  
  private Resource resource;
  private long lastModified;
  private Throwable exception;

  private PathTree<FilterSet> node;
  
  public FilterSet
    (Resource containerResource
    ,PathTree<FilterSet> node
    ,FilterConfig config
    )
    throws IOException
  { 
    this.config=config;
    Path path=node.getPath();
    
    try
    { 
        
      Resource controlResource
        =containerResource.asContainer().getChild(controlFileName);
      if (controlResource.exists())
      { loadResource(controlResource,path);
      }
      
      pathContextFilter.setPath(path);
      pathContextFilter.setContainer(containerResource);
      pathContextFilter.init(config);
      
      node.set(this);
      this.node=node;
      
      Resource errorResource
        =containerResource.asContainer().getChild(controlFileName+".err");
      try
      {
        if (exception==null)
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
          pout.println(exception.toString());
          pout.println("Stack trace ----------------------------------------");
          exception.printStackTrace(pout);
          pout.flush();
          out.flush();
          out.close();
        }
      }
      catch (IOException y)
      { System.err.println("Controller: error writing control file success: "+y);
      }
      
    }
    catch (Throwable x)
    { 
      
      Resource errorResource
        =containerResource.asContainer().getChild(controlFileName+".err");
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
      
      loadError(node.getPath(), x);
      
    }      
  }
  
  
  public boolean isNull()
  { return node==null;
  }
  
  public List<AutoFilter> getEffectiveFilters()
  { return effectiveFilters;
  }
  
  public Throwable getException()
  { return exception;
  }
  
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
    if (pathContextFilter!=null)
    { pathContextFilter.destroy();
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
    
    if (pathContextFilter!=null)
    { effectiveFilters.add(pathContextFilter);
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
        filter.setContainer(resource.getParent());
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
    { filter.init(new StandardFilterConfig(null,config,null));
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
