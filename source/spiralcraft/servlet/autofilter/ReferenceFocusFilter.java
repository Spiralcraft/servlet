//
//Copyright (c) 1998,2008 Michael Toth
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

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.ThreadContextual;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.servlet.autofilter.spi.FocusFilter;

import spiralcraft.data.DataException;
import spiralcraft.data.Type;
import spiralcraft.data.persist.AbstractXmlObject;
//import spiralcraft.data.persist.PersistentFocusProvider;

import spiralcraft.common.LifecycleException;


/**
 * <p>Creates a Focus from an persistent reference- ie. a typed object
 *   optionally represented by an existing persistent resource.
 * </p>
 * 
 * <p>Will create one instance per unit of the specified Scope.
 * </p>
 */
public class ReferenceFocusFilter<Treferent,Tfocus>
    extends FocusFilter<Tfocus>
{
  
  public enum Scope
  { 
    SESSION
    ,APPLICATION
    ,CONSTANT
  }

  private URI instanceURI;
  private Type<?> type;
  private Scope scope=Scope.APPLICATION;
  private ThreadLocalChannel<Tfocus> transientBinding;
  private Channel<Tfocus> subject;
  private Focus<?> parentFocus;
  private String attributeName;
  private boolean threaded=false;

  
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

  
  /**
   * Specify the Scope in which each instance will be made available
   * 
   * <ul>
   *   <li>SESSION: A unique instance will be made available for each session
   *   </li>
   *   <li>APPLICATION: A mutable, shared instance will be made available for 
   *     the entire application.
   *   </li>
   *   <li>CONSTANT: An unmodifiable instance will be made available for
   *     the entire application, and may be retrieved at load-time.
   *   </li>
   * </ul>
   * 
   * @param scope
   */
  public void setScope(Scope scope)
  { this.scope=scope;
  }


  /**
   * Called -once- to create the Focus
   */
  @Override
  protected Focus<Tfocus> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    URI attributeURI=instanceURI;
    if (attributeURI==null)
    { attributeURI=type.getURI();
    }
    
    this.attributeName
      =this.getPath().format("/")+"!"+attributeURI;
    
    this.parentFocus=parentFocus;
    
    Focus<Tfocus> focus;
    
    switch (scope)
    {
      case CONSTANT:
        focus=createConstantFocus(parentFocus);
        break;
      case APPLICATION:
        focus=createStableFocus(parentFocus);
        break;
      case SESSION:
        focus=createTransientFocus(parentFocus);
        break;
      default:
        throw new BindException("Unknown scope "+scope);
    }
    
    threaded
      =ThreadContextual.class.isAssignableFrom
        (focus.getSubject().getContentType());
    subject=focus.getSubject();
    return focus;
  }

  private Focus<Tfocus> createConstantFocus(Focus<?> parentFocus)
    throws BindException
  {
    Focus<Tfocus> focus=createStableFocus(parentFocus);
    SimpleChannel<Tfocus> constantChannel
      =new SimpleChannel<Tfocus>
        (focus.getSubject().getReflector()
        ,focus.getSubject().get()
        ,true
        );
    
    subject=constantChannel;
    if (parentFocus!=null)
    { return parentFocus.chain(constantChannel);
    }
    else
    { return new SimpleFocus<Tfocus>(constantChannel);
    }
  }
  
  private Focus<Tfocus> createStableFocus(Focus<?> parentFocus)
    throws BindException
  {
    FocusHolder stableFocusHolder
      =new FocusHolder(parentFocus);
    
    if (debug)
    { log.fine(stableFocusHolder.getFocus().toString());
    }
    
    subject=stableFocusHolder.getFocus().getSubject();
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
    subject=transientBinding;
    
    return new SimpleFocus<Tfocus>
      (parentFocus,transientBinding);
    
  }
  


  
  class FocusHolder
  {
    // XXX Register as session event listener
    
    private Focus<Tfocus> referencedFocus;
    private AbstractXmlObject<Treferent,Tfocus> reference;
    
//    private PersistentFocusProvider<Treferent,Tfocus> focusProvider;
    
    @SuppressWarnings("unchecked") // Cast reference to contain Tfocus
    public FocusHolder(Focus<?> parentFocus)
      throws BindException
    { 
      if (parentFocus==null)
      { parentFocus=new SimpleFocus(null);
      }
      
      reference=(AbstractXmlObject<Treferent,Tfocus>) 
        AbstractXmlObject.<Treferent>activate
          (type!=null?type.getURI():null,instanceURI,null,parentFocus);
      
      referencedFocus=(Focus<Tfocus>) reference.getFocus();
      
//      focusProvider=new PersistentFocusProvider<Treferent,Tfocus>
//        (createReference());
      
//      referencedFocus=focusProvider.createFocus(parentFocus);
      
    }
    
    
    public Focus<Tfocus> getFocus()
    { return referencedFocus;
    }
    
    public void destroy()
      throws LifecycleException
    { reference.stop();
    }
  }




  @Override
  protected void popSubject(HttpServletRequest request)
  {
    if (threaded)
    { ((ThreadContextual) subject.get()).pop();
    }
    if (scope==Scope.SESSION)
    { transientBinding.pop();
    }
    
  }
  
  

  @Override
  protected void pushSubject
    (HttpServletRequest request
    ,HttpServletResponse response
    ) 
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
        // Avoid race condition
        synchronized (session)
        {
          targetFocusHolder
            =(FocusHolder) session.getAttribute(attributeName);
          if (targetFocusHolder==null)
          {
            targetFocusHolder=new FocusHolder(parentFocus);
            session.setAttribute(attributeName, targetFocusHolder);
            if (debug)
            { 
              log.fine
                ("Created session scoped reference "+instanceURI+" for session "
                +session.getId()
                );
            }
          }
          else
          {
            if (debug)
            {
              log.fine
                ("Averted race condition creating session scoped reference "
                +instanceURI+" for session "+session.getId()
                );
            }
          }
        }
      }
      transientBinding.push(targetFocusHolder.getFocus().getSubject().get()); 
      
    }
    if (threaded)
    { ((ThreadContextual) subject.get()).push();
    }
  }


}

