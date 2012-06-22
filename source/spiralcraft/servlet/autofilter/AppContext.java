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
import spiralcraft.lang.util.LangUtil;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.common.ContextualException;
import spiralcraft.common.Lifecycle;
import spiralcraft.common.LifecycleException;

/**
 * <p>Defines functionality common to an entire web application
 * </p>
 * 
 * @author mike
 *
 */
public class AppContext
  extends AbstractChainableContext
    implements Lifecycle
{

  private Container contentResource;
  private URI contentBaseURI;
  private URI codeBaseURI;
  private URI defaultCodeBaseURI;
  private PathContext baseContext;
  private AutoFilter[] filters;
  
  
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
   * The filter chain to be used for all requests prior to calling the
   *   external filter chain.
   * 
   * @param placeContext
   */
  public void setFilters(AutoFilter[] filters)
  { this.filters=filters;
  }

  
  /**
   * Given a path relative to this AppContext,
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
    ret=Resolver.getInstance().resolve
      (getEffectiveCodeBaseURI().resolve(relativePath));
    if (ret!=null && ret.exists())
    { return ret;
    }
    else
    { ret=null;
    }
    
    
    if (ret==null && baseContext!=null)
    { ret=baseContext.resolveCode(relativePath);
    }
    
    return ret;
  }

  private URI getEffectiveCodeBaseURI()
  { return codeBaseURI!=null?codeBaseURI:defaultCodeBaseURI;
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
  

  
  AutoFilter[] getFilters()
  { 
    return ArrayUtil.concat
      (AutoFilter[].class
      ,filters
      ,baseContext!=null?baseContext.getFilters():null
      );
  }
  
  @Override
  protected Focus<?> bindImports(Focus<?> chain)
    throws ContextualException
  { 
    if (baseContext!=null)
    { chain=baseContext.bind(chain);
    }
    chain=chain.chain(LangUtil.constantChannel(this));

    
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
    super.popLocal();
    
  }  
  @Override
  public void start()
    throws LifecycleException
  { 
    if (baseContext!=null)
    { baseContext.start();
    }
    
  }

  @Override
  public void stop()
    throws LifecycleException
  {
    if (baseContext!=null)
    { baseContext.stop();
    }
  }

  @Override
  public String toString()
  { return super.toString()+": base="+baseContext;
  }
  
}
