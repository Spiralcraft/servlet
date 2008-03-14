package spiralcraft.servlet.autofilter;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLogger;
import spiralcraft.registry.Registry;

import spiralcraft.data.DataException;
import spiralcraft.data.Type;
import spiralcraft.data.builder.BuilderType;
import spiralcraft.data.persist.AbstractXmlObject;
import spiralcraft.data.persist.PersistenceException;
import spiralcraft.data.persist.PersistentFocusProvider;
import spiralcraft.data.persist.XmlAssembly;
import spiralcraft.data.persist.XmlBean;

import spiralcraft.builder.LifecycleException;


/**
 * Creates a Focus from an Assembly via an spiralcraft.data.persist.XmlAssembly
 */
public class ReferenceFocusFilter<Treferent,Tfocus>
    extends FocusFilter<Tfocus>
{
  private final ClassLogger log=ClassLogger.getInstance(ReferenceFocusFilter.class);
  
  public enum Scope
  { 
    SESSION
    ,APPLICATION
  };

  private URI instanceURI;
  private Type<?> type;
  private Scope scope=Scope.APPLICATION;
  private ThreadLocalChannel<Tfocus> transientBinding;
  private FocusHolder stableFocusHolder;
  private Focus<?> parentFocus;
  private String attributeName;

  
  public void setTypeURI(URI typeURI)
  { 
    try
    { type=Type.resolve(typeURI);
    }
    catch (DataException x)
    { throw new IllegalArgumentException(x);
    }
  }
  
  public void setInstanceType(Type<?> type)
  { this.type=type;
  }
  
  public void setInstanceURI(URI instanceURI)
  { this.instanceURI=instanceURI;
  }

  
  public void setScope(Scope scope)
  { this.scope=scope;
  }



  private AbstractXmlObject<Treferent,?> createReference()
    throws BindException
  {
    AbstractXmlObject<Treferent,?> reference;
    
    if (type instanceof BuilderType)
    { 
      try
      {
        XmlAssembly<Treferent> assy
          =new XmlAssembly<Treferent>(type.getURI(),instanceURI);
        assy.register(Registry.getLocalRoot());
        assy.start();
        reference=assy;
        
      }
      catch (PersistenceException x)
      { throw new BindException("Error creating XmlAssembly: "+x,x);
      }
      catch (LifecycleException x)
      { throw new BindException("Error creating XmlAssembly: "+x,x);
      }
    }
    else
    {
      try
      {
        XmlBean<Treferent> bean
          =new XmlBean<Treferent>(type.getURI(),instanceURI);
        bean.register(Registry.getLocalRoot());
        bean.start();
        reference=bean;
        
      }
      catch (PersistenceException x)
      { throw new BindException("Error creating XmlBean: "+x,x);
      }
      catch (LifecycleException x)
      { throw new BindException("Error starting XmlBean: "+x,x);
      }
      
    }
    return reference;
    
  }

  /**
   * Called -once- to create the Focus
   */
  protected Focus<Tfocus> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    this.attributeName=this.getPath().format("/")+"!"+type.getURI();
    
    this.parentFocus=parentFocus;
    switch (scope)
    {
      case APPLICATION:
        return createStableFocus(parentFocus);
      case SESSION:
        return createTransientFocus(parentFocus);
    }
    throw new BindException("Unknown scope "+scope);
  }

  private Focus<Tfocus> createStableFocus(Focus<?> parentFocus)
    throws BindException
  {
    stableFocusHolder
      =new FocusHolder(parentFocus);
    
    log.fine(stableFocusHolder.getFocus().toString());
    return stableFocusHolder.getFocus();

  
  }

  private Focus<Tfocus> createTransientFocus(Focus<?> parentFocus)
    throws BindException
  {
    FocusHolder targetFocusHolder
      =new FocusHolder(parentFocus);
    
    transientBinding
      =new ThreadLocalChannel<Tfocus>
        (targetFocusHolder.getFocus().getSubject().getReflector());

    return new SimpleFocus<Tfocus>
      (parentFocus,transientBinding);
    
  }
  


  
  class FocusHolder
  {
    // XXX Register as session event listener
    
    private Focus<Tfocus> referencedFocus;
    private PersistentFocusProvider<Treferent,Tfocus> focusProvider;
    
    @SuppressWarnings("unchecked") // Runtime class check for Focus provider
    public FocusHolder(Focus<?> parentFocus)
      throws BindException
    { 
      focusProvider=new PersistentFocusProvider<Treferent,Tfocus>
        (createReference());
      
      referencedFocus=focusProvider.createFocus(parentFocus);
      
    }
    
    
    public Focus<Tfocus> getFocus()
    { return referencedFocus;
    }
    
    public void destroy()
      throws LifecycleException
    { focusProvider.getReference();
    }
  }




  @Override
  protected void popSubject(HttpServletRequest request)
  {
    if (scope==Scope.SESSION)
    { transientBinding.pop();
    }
    
  }
  
  

  @Override
  protected void pushSubject(HttpServletRequest request) 
    throws BindException
  {
    if (scope==Scope.SESSION)
    {

      // From a session-bound Focus, we need to pin the data value
      //   in the chain via ThreadLocal for the duration of the request,
      //   because we can't bind directly to the session focus.
      
      // Maybe ThreadLocalFocus can hold a channel in ThreadLocal to 
      //   respond to get()/set(), so a session channel is being exposed not
      //   just the data available that does not change during a request.
      
      // A better way to do this is to 
      
      HttpSession session=request.getSession();
      FocusHolder targetFocusHolder
        =(FocusHolder) session.getAttribute(attributeName);
      
      if (targetFocusHolder==null)
      { 
        targetFocusHolder=new FocusHolder(parentFocus);
        session.setAttribute(attributeName, targetFocusHolder);
      }
      transientBinding.push(targetFocusHolder.getFocus().getSubject().get());      
    }
    
  }


}

