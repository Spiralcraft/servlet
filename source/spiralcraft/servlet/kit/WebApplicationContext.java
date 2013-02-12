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
package spiralcraft.servlet.kit;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;


import spiralcraft.bundle.Bundle;
import spiralcraft.bundle.BundleClassLoader;
import spiralcraft.bundle.Library;
import spiralcraft.common.ContextualException;
import spiralcraft.common.LifecycleException;
import spiralcraft.lang.Context;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLog;
import spiralcraft.time.Scheduler;
import spiralcraft.util.ContextDictionary;
import spiralcraft.util.Path;
import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.context.Authority;
import spiralcraft.vfs.context.ContextResourceMap;
import spiralcraft.vfs.context.Redirect;

/**
 * Establishes access to VFS resources, classes and package bundles 
 *   distributed with a web application.
 * 
 * @author mike
 *
 */
public class WebApplicationContext
  implements Context
{
  protected final ClassLog log=ClassLog.getInstance(getClass());

  private Scheduler scheduler;

  protected Focus<?> focus;  

  protected final ContextDictionary contextDictionary
    =new ContextDictionary
      (ContextDictionary.getInstance()
      ,new HashMap<String,String>()
      ,true
      );
  
  protected final ContextResourceMap contextResourceMap
    = new ContextResourceMap();
  

  protected BundleClassLoader bundleClassLoader;

  private ThreadLocal<ClassLoader> oldContextClassLoader
    =new ThreadLocal<ClassLoader>();
  
  protected boolean debug=false;
  
  protected URI instanceRootURI;
  protected URI dataURI=URI.create("data/");
  protected URI configURI=URI.create("config/");
  protected URI filesURI=URI.create("files/");
  protected URI codeURI=URI.create("webui/");
  protected URI themeURI=URI.create("webui/theme/");  
  
  protected Resource publishRoot;
  protected Resource publishOverlay;

  
  private HashMap<String,Path> bundleMountPointMap
    =new HashMap<String,Path>();
  {
    bundleMountPointMap.put("war-js",Path.create("js"));
    bundleMountPointMap.put("war-css",Path.create("css"));
    bundleMountPointMap.put("war-public",Path.create(""));
    bundleMountPointMap.put("war-public-images",Path.create("images"));
  }
  
  
  protected Authority rootAuthority;
  protected Authority codeAuthority;
  
  
  /**
   * <p>The uri of the root directory for the data, configuration 
   *   and process info for this app instance.
   * </p>
   * @param instanceRootURI
   */
  public void setInstanceRootURI(URI instanceRootURI)
  { this.instanceRootURI=instanceRootURI;
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
  
  
    @Override
  public void push()
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
  
  @Override
  public void pop()
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
  
  @Override
  public Focus<?> bind(Focus<?> focusChain)
    throws ContextualException
  { 
    focus=focusChain;
    return focusChain;
  }
  
  protected void initContextResourceMap()
    throws ContextualException
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
    
    codeAuthority=new Authority("code",contextURI.resolve(codeURI));
    contextResourceMap.put(codeAuthority);
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
      throw new ContextualException
        ("Error binding Controller in "
        +contextURI.toString()
        ,x
        );
    }
    
    
  }  
  
  protected void initLibrary()
    throws ContextualException
  {
    try
    {
      Resource packagesResource
        =Resolver.getInstance()
          .resolve(publishRoot.getURI().resolve("WEB-INF/packages"));
    
      Container packagesContainer
        =packagesResource.asContainer();
    
      if (debug)
      { log.fine("Initializing library for "+toString());
      }
      if (packagesContainer!=null)
      { Library.set(new Library(packagesContainer));
      }
      
      Library library=Library.get();
      if (library!=null)
      {
        // TODO: We need make loading system bundles a context parameter, and/or
        //   express isolation as part of the package library configuration
        Bundle[] bundles
          =library.getAllBundles();
      
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
            if (debug)
            { log.fine("Mounted "+bundle.getBundleURI()+" to "+mountPoint);
            }
          }
        
          if (bundle.getBundleName().equals("war-classes"))
          { classBundles.add(bundle.getAuthorityName());
          }
          else if (bundle.getBundleName().equals("war-lib"))
          { jarLibBundles.add(bundle.getAuthorityName());
          }
          else if (bundle.getBundleName().equals("war-webui"))
          { 
            String packageName=bundle.getPackage().getName();
            
            Resource overlay
              =Resolver.getInstance().resolve
                (URIUtil.ensureTrailingSlash
                  (URIUtil.addPathSegment
                    (codeAuthority.getRootURI(),packageName)
                  )
                );
            if (!overlay.exists())
            {
              codeAuthority.mapPath
                (packageName
                ,new Redirect
                  (URI.create(packageName)
                  ,bundle.getBundleURI()
                  )
                );
              if (debug)
              {
                log.fine("Mounted "
                    +bundle.getBundleURI()
                    +" to context://code/"
                    +packageName
                    );
              }
              
            }
            else
            {
              if (debug)
              {
                log.fine("Did not mount "
                    +bundle.getBundleURI()
                    +" to context://code/"
                    +packageName
                    +" because mount point already exists"
                    );
              }
              // TODO: Verify that the mount point eventually 
              //   leads to the bundle
            }
          }
        }
        
        bundleClassLoader
          =new BundleClassLoader
            (classBundles.toArray(new String[classBundles.size()])
            ,jarLibBundles.toArray(new String[jarLibBundles.size()])
            );
        bundleClassLoader.start();
      // bundleClassLoader.setLogLevel(Level.FINE);
      }
    }
    catch (IOException x)
    { throw new ContextualException("IOException initializing libraries",x);
    } 
    catch (LifecycleException x)
    { throw new ContextualException("LifecycleException initializing libraries",x);
    }
  }
  
  public void stop()
    throws LifecycleException
  { 
    if (bundleClassLoader!=null)
    { bundleClassLoader.stop();
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
  
  
  private URI cleanURI(URI uri)
  { 
    if (!uri.getPath().endsWith("/"))
    { return URIUtil.ensureTrailingSlash(uri);
    }
    else
    { return uri;
    }
  }
}
