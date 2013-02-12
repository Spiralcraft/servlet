package spiralcraft.servlet.task;

import java.io.IOException;
import java.net.URI;


import spiralcraft.app.PlaceContext;
import spiralcraft.classloader.Archive;
import spiralcraft.common.ContextualException;
import spiralcraft.common.LifecycleException;
import spiralcraft.data.persist.AbstractXmlObject;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.Level;
import spiralcraft.servlet.kit.WebApplicationContext;
import spiralcraft.servlet.util.ServletURLClassLoader;
import spiralcraft.servlet.util.WARClassLoader;
import spiralcraft.text.html.URLEncoder;
import spiralcraft.util.ContextDictionary;
import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

public class WebBatchContext
  extends WebApplicationContext
{
  
  private ClassLoader contextClassLoader;
  private boolean useURLClassLoader;
  private Resource[] libraryResources;
  private final ThreadLocal<ClassLoader> lastLoader
    =new ThreadLocal<ClassLoader>();
  
  
  private URI rootPlaceURI;
  private PlaceContext rootPlace;
  private Expression<?>[] rootPublications;
  
  @Override
  public Focus<?> bind(Focus<?> focus) 
    throws ContextualException
  {
    Focus<?> sfocus=super.bind(focus);
    if (publishRoot==null)
    { 
      try
      { publishRoot=Resolver.getInstance().resolve("context:/war/");
      }
      catch (UnresolvableURIException x)
      { throw new RuntimeException(x);
      }
    }
    
    loadWAR();
    
    pushClassLoader();
    try
    {
      resolveResourceVolumes();
      initContextResourceMap();
      initLibrary();
      push();
      try
      { 
        if (rootPublications!=null)
        { 
          for (Expression<?> expr:rootPublications)
          { focus.addFacet(focus.chain(focus.bind(expr)));
          }
        }
        sfocus=initPlace();
        if (rootPlace!=null)
        { rootPlace.push();
        }
      }
      finally
      { pop();
      }
    }
    finally
    { popClassLoader();
    }
    
   
    return sfocus;
  }
  
  public void setRootPlaceURI(URI rootPlaceURI)
  { this.rootPlaceURI=rootPlaceURI;
  }
  
  /**
   * A set of expressions that will be published into the focus chain and
   *   made available to the root PlaceContext
   *   
   * @param publications
   */
  public void setRootPublications(Expression<?>[] publications)
  { this.rootPublications=publications;
  }
  
  @Override
  public void push()
  {
    pushClassLoader();
    super.push();
    if (rootPlace!=null)
    { rootPlace.push();
    }
  }
  
  @Override
  public void pop()
  {
    if (rootPlace!=null)
    { rootPlace.pop();
    }
    super.pop();
    popClassLoader();
  }
  
  private void pushClassLoader()
  {
    if (contextClassLoader!=null)
    { 
      lastLoader.set(Thread.currentThread().getContextClassLoader());
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  private void popClassLoader()
  {
    if (contextClassLoader!=null)
    { 
      Thread.currentThread().setContextClassLoader(lastLoader.get());
      lastLoader.remove();
    }
  }
  
  /**
   * <p>Resolve locations for various contextual resource volumes
   * </p>
   * 
   * @param context
   */
  private void resolveResourceVolumes()
    throws ContextualException
  {
    
    ContextDictionary dict=ContextDictionary.getInstance();
    

    
    URI webInfRoot=publishRoot.getURI().resolve("WEB-INF/");
    
    if (instanceRootURI==null)
    {

      String instanceRoot
        =dict.find("spiralcraft.instance.rootURI");

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
    }
    else
    { 
      try
      { instanceRootURI=Resolver.getInstance().resolve(instanceRootURI).getURI();
      }
      catch (IOException x)
      { throw new ContextualException("Error resolving "+instanceRootURI);
      }
    }
    
    log.info("web instance root: "+instanceRootURI);
    dataURI=resolveResourceVolume
      (dict,instanceRootURI,dataURI,"spiralcraft.instance.dataURI");
    log.info("context://data = "+dataURI);
    configURI=resolveResourceVolume
      (dict,instanceRootURI,configURI,"spiralcraft.instance.configURI");
    log.info("context://config = "+configURI);
    filesURI=resolveResourceVolume
      (dict,instanceRootURI,filesURI,"spiralcraft.instance.filesURI");
    log.info("context://files = "+filesURI);
    codeURI=resolveResourceVolume
      (dict,webInfRoot,codeURI,"spiralcraft.instance.codeURI");
    log.info("context://code = "+codeURI);
    themeURI=resolveResourceVolume
      (dict,webInfRoot,themeURI,"spiralcraft.instance.themeURI");
    log.info("context://theme = "+themeURI);
    
  }
  
  private URI resolveResourceVolume
    (ContextDictionary dict
    ,URI rootURI
    ,URI defaultURI
    ,String propName
    )
  {
    String uriParam
      =dict.find(propName);
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
  
  private void loadWAR()
  {
    try
    {
      Resource docRoot=publishRoot;
      if (docRoot.asContainer()!=null)
      {
        Resource warRoot=docRoot.asContainer().getChild("WEB-INF");
        if (warRoot.exists())
        { 
          if (!useURLClassLoader)
          {
            if (debug)
            {
              log.log
                (Level.DEBUG,"Loading WARClassloader from "+warRoot.getURI());
            }            
            WARClassLoader contextClassLoader=new WARClassLoader(warRoot);
            if (libraryResources!=null)
            {
              for (Resource resource: libraryResources)
              { 
                Archive[] archives;
                
                try
                { 
                  archives=Archive.fromLibrary(resource);
                  if (archives.length==0)
                  { log.info("Library is empty: "+resource.getURI());
                  }
                  for (Archive archive: archives)
                  { contextClassLoader.addPrecedentArchive(archive);                  
                  }
                }
                catch (IOException x)
                { log.log(Level.WARNING,"Failed to load archive "+resource.getURI(),x);
                }
              }
            }
            
            if (debug)
            { contextClassLoader.setDebug(true);
            }
            contextClassLoader.start();
            this.contextClassLoader=contextClassLoader;
          }
          else
          {
            ServletURLClassLoader contextClassLoader
              =new ServletURLClassLoader(warRoot,libraryResources);
            this.contextClassLoader=contextClassLoader;
          }
        }
        else
        {
          log.log
            (Level.INFO,warRoot.getURI()+" does not exist "
              +", not loading WAR ClassLoader"
           );
        }
      }
      else
      { 
        log.log
          (Level.SEVERE,"Document root "
              +publishRoot.getURI()
              +" is not a valid directory"
              +", not loading WAR ClassLoader"
           );
      }
    }
    catch (IOException x)
    { log.log(Level.SEVERE,"Error loading WAR ClassLoader: "+x.toString());
    }
    catch (LifecycleException x)
    { log.log(Level.SEVERE,"Error loading WAR ClassLoader: "+x.toString());
    }
  }  
  
  private Focus<?> initPlace()
    throws ContextualException
  {
    if (rootPlaceURI!=null)
    { 
      AbstractXmlObject<PlaceContext,?> placeContainer
        =AbstractXmlObject.<PlaceContext>activate(rootPlaceURI,focus);
      rootPlace=placeContainer.get();
      return placeContainer.getFocus();
    }
    return focus;
  }
  
}