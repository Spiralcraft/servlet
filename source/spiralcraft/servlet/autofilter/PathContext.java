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

import spiralcraft.lang.Focus;
import spiralcraft.lang.kit.AbstractChainableContext;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.Path;
import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.app.PlaceContext;
import spiralcraft.common.ContextualException;
import spiralcraft.common.Lifecycle;
import spiralcraft.common.LifecycleException;

/**
 * <p>Defines the functionality of a subtree addressed by a URI path
 * </p>
 * 
 * @author mike
 *
 */
public class PathContext
  extends AbstractChainableContext
    implements Lifecycle
{

  private Path absolutePath;
  private String absolutePathString;
  private Container contentResource;
  private URI contentBaseURI;
  private URI codeBaseURI;
  private URI defaultCodeBaseURI;
  private PathContext baseContext;
  private PlaceContext placeContext;
  private AutoFilter[] filters;
  // private PathContext parent;
  
  /**
   * The path from the context root
   * 
   * @return
   */
  public Path getAbsolutePath()
  { return absolutePath;
  }
  
  /**
   * The URI that contains the base content resources (publicly accessible
   *   files) for this path.
   * 
   * @param container
   */
  public void setContentBaseURI(URI contentBaseURI)
  { this.contentBaseURI=URIUtil.ensureTrailingSlash(contentBaseURI);
  }
  
  /**
   * The URI that contains the base code resources (non-publicly accessible
   *   files) for this path.
   * 
   * @param container
   */
  public void setCodeBaseURI(URI codeBaseURI)
  { this.codeBaseURI=URIUtil.ensureTrailingSlash(codeBaseURI);
  }
  
  /**
   * Provides a default set of resources and functionality that can be
   *   extended or overridden by this PathContext
   * 
   * @param baseContext
   */
  public void setBaseContext(PathContext baseContext)
  { this.baseContext=baseContext;
  }

  /**
   * Specify a PlaceContext which defines the application model for the
   *   subtree rooted at this PathContext.
   * 
   * @param placeContext
   */
  public void setPlaceContext(PlaceContext placeContext)
  { this.placeContext=placeContext;
  }

  /**
   * The filter chain to be used for all requests prior to calling the
   *   external filter chain.
   * 
   * @param placeContext
   */
  public void setFilters(AutoFilter[] filters)
  { this.filters=filters;
  }

  /**
   * Relativize the given absolute path against the absolute path of
   *   this PathContext.
   *   
   * @param absolutePath
   * @return a path that is relative to this PathContext
   */
  public String relativize(String absolutePath)
  {
    if (!absolutePath.startsWith(absolutePathString))
    { 
      throw new IllegalArgumentException
        (absolutePath+" is not in "+absolutePathString);
    }
    String relativePath=absolutePath.substring(absolutePathString.length());
    return relativePath;
  }
  
  /**
   * Given a path relative to this PathContext,
   *   find the associated content resource.
   * 
   * @param absolutePath
   * @return
   */
  public Resource resolveContent(String relativePath)
    throws IOException
  { 
  
    Resource ret=null;
    if (contentResource!=null)
    { 
      ret=Resolver.getInstance().resolve
        (contentResource.getURI().resolve(relativePath));
      if (ret!=null && ret.exists())
      { return ret;
      }
      else
      { ret=null;
      }
    }
    
    if (ret==null && contentBaseURI!=null)
    {
      ret=Resolver.getInstance().resolve
        (contentBaseURI.resolve(relativePath));
      if (ret!=null && ret.exists())
      { return ret;
      }
      else
      { ret=null;
      }
    }
    
    if (ret==null && baseContext!=null)
    { ret=baseContext.resolveContent(relativePath);
    }
    return ret;
  }

  /**
   * Given a path relative to this PathContext,
   *    find the associated code resource.
   * 
   * @param absolutePath
   * @return
   */
  public Resource resolveCode(String relativePath)
    throws IOException
  { 
  
    Resource ret=null;
    if (codeBaseURI!=null)
    {
      ret=Resolver.getInstance().resolve
        (codeBaseURI.resolve(relativePath));
      if (ret!=null && ret.exists())
      { return ret;
      }
      else
      { ret=null;
      }
    }
    else if (defaultCodeBaseURI!=null)
    {
      ret=Resolver.getInstance().resolve
        (defaultCodeBaseURI.resolve(relativePath));
      if (ret!=null && ret.exists())
      { return ret;
      }
      else
      { ret=null;
      }
    }
    
    
    if (ret==null && baseContext!=null)
    { ret=baseContext.resolveCode(relativePath);
    }
    return ret;
  }

  /**
   * Return a URI to the container that provides the context for
   *   the relative path.
   * 
   * @param relativePath
   * @return
   */
  URI mapRelativePath(String relativePath)
  { return null;
  }
  
  /**
   * The path relative to the container (servlet) context
   * 
   * @param path
   */
  void setAbsolutePath(Path path)
  { 
    this.absolutePath=path;
    this.absolutePathString=absolutePath.format("/");
  }
  
  /**
   * The directory that would normally be mapped to this PathContext by the
   *   servlet container. 
   * 
   * @param container
   */
  void setContentResource(Container container)
  { this.contentResource=container;
  }
  
  void setDefaultCodeBaseURI(URI defaultCodeBaseURI)
  { this.defaultCodeBaseURI=defaultCodeBaseURI;
  }
  

  void setParent(PathContext parentContext)
  { // this.parent=parentContext;
  }
  
  
  AutoFilter[] getFilters()
  { 
    return 
      (filters!=null && baseContext!=null)
      ?ArrayUtil.concat(filters,baseContext.getFilters())
      :filters==null
      ?baseContext.getFilters()
      :filters;
  }
  
  @Override
  protected Focus<?> bindImports(Focus<?> chain)
    throws ContextualException
  { 
    if (baseContext!=null)
    { chain=baseContext.bind(chain);
    }
    chain=chain.chain(new SimpleChannel<PathContext>(this,true));
    
    if (placeContext!=null)
    { chain(placeContext);
    }
    return chain;
  }

  @Override
  protected void pushLocal()
  {
    super.pushLocal();
    if (baseContext!=null)
    { baseContext.push();
    }
  }
  
  @Override
  protected void popLocal()
  {
    if (baseContext!=null)
    { baseContext.pop();
    }
    
  }  
  @Override
  public void start()
    throws LifecycleException
  { 
    if (baseContext!=null)
    { baseContext.start();
    }
    if (placeContext!=null)
    { placeContext.start();
    }
    
  }

  @Override
  public void stop()
    throws LifecycleException
  {
    if (placeContext!=null)
    { placeContext.stop();
    }
    if (baseContext!=null)
    { baseContext.stop();
    }
  }

  @Override
  public String toString()
  { return super.toString()+": path="+absolutePath+", place="+placeContext+", base="+baseContext;
  }
  
}
