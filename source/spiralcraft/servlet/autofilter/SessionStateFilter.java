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

import spiralcraft.servlet.autofilter.spi.FocusFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.spi.ViewCache;
import spiralcraft.lang.spi.ViewState;
import spiralcraft.lang.util.LangUtil;

/**
 * <p>Provides hooks to track http session state transitions
 * </p>
 * 
 * @author mike
 *
 */
public class SessionStateFilter<T>
  extends FocusFilter<T>
{
  
  
  private ThreadLocalChannel<T> sessionChannel;
  private Channel<HttpSession> httpSessionChannel;
  private String attributeName;
  private String attributeSuffix;
  private Reflector<T> reflector;
  private Binding<T> constructor;
  private Binding<?> onConstruct;
  private Binding<?> onEstablish;
  private Binding<?> onExpire;
  private Binding<?> onRequest;
  private Binding<?> afterRequest;
  private Binding<?> onBind;
  private Binding<?> onDestroy;
  private ViewCache viewCache;
  private Binding<?>[] triggers;
  
  
  /**
   * The suffix that will be appended to the HttpSession attribute name 
   *   to differentiate this Filter's value from the values stored by other
   *   filters.
   * 
   * @param attributeSuffix
   */
  public void setAttributeSuffix(String attributeSuffix)
  { this.attributeSuffix=attributeSuffix;
  }
  
  /**
   * Creates the application object that will be stored in the session
   * 
   * @param constructor
   */
  public void setConstructor(Binding<T> constructor)
  { this.constructor=constructor;
  }
  

  public void setOnBind(Binding<?> onBind)
  { this.onBind=onBind;
  }
  
  public void setOnDestroy(Binding<?> onDestroy)
  { this.onDestroy=onDestroy;
  }

  /**
   * <p>An expression to be evaluated when this session is constructed
   * </p>
   *   
   * @param onExpire
   */
  public void setOnConstruct(Binding<?> onConstruct)
  { this.onConstruct=onConstruct;
  } 

  /**
   * <p>An expression to be evaluated when this session is established 
   *   (requested using a client cookie).
   * </p>
   *   
   * @param onExpire
   */
  public void setOnEstablish(Binding<?> onEstablish)
  { this.onEstablish=onEstablish;
  } 

  /**
   * <p>An expression to be evaluated when this session is forced to expire
   *   during a request (e.g. the user logs out). This will not be called
   *   when the session times out.
   * </p>
   *   
   * @param onExpire
   */
  public void setOnExpire(Binding<?> onExpire)
  { this.onExpire=onExpire;
  } 

  /**
   * <p>An expression to be evaluated before every request
   * </p>
   *   
   * @param onExpire
   */
  public void setOnRequest(Binding<?> onRequest)
  { this.onRequest=onRequest;
  } 
  
  /**
   * <p>An expression to be evaluated after every request
   * </p>
   *   
   * @param onExpire
   */
  public void setAfterRequest(Binding<?> afterRequest)
  { this.afterRequest=afterRequest;
  } 
  
  /**
   * Triggers run after each request and can contain Accumulator expressions
   *   (e.g. count, sum, etc.,.) which keep tallying for the duration of
   *   the session.
   * 
   * @param triggers
   */
  public void setTriggers(Binding<?>[] triggers)
  { this.triggers=triggers;
  }
  
  /**
   * Called -once- to create the Focus
   */
  @Override
  protected Focus<T> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    if (onDestroy!=null)
    { onDestroy.bind(parentFocus);
    }
    httpSessionChannel=LangUtil.assertChannel(HttpSession.class,parentFocus);
    
    constructor.bind(parentFocus);
    reflector=constructor.getReflector();
    
    this.attributeName=this.getPath().format("/")+"!"+
      reflector.getTypeURI()+(attributeSuffix!=null?attributeSuffix:"");
      
    sessionChannel
      =new ThreadLocalChannel<T>
        (reflector)
     {
       @Override
       public boolean store(T val)
       {
         if (super.store(val))
         { updateSessionValue(val);
           return true;
         }
         return false;
       }
     };
    
    Focus<T> focus=parentFocus.chain(sessionChannel);
    if (onConstruct!=null)
    { onConstruct.bind(focus);
    }
    if (onEstablish!=null)
    { onEstablish.bind(focus);
    }
    if (onExpire!=null)
    { onExpire.bind(focus);
    }
    if (onRequest!=null)
    { onRequest.bind(focus);
    }
    if (afterRequest!=null)
    { afterRequest.bind(focus);
    }
    
    if (triggers!=null)
    {
      viewCache=new ViewCache(focus);
      
      Focus<?> viewCacheFocus=focus.chain(focus.getSubject());
      viewCacheFocus.addFacet(viewCache.bind(viewCacheFocus));
      
      for (Binding<?> trigger: triggers)
      { trigger.bind(viewCacheFocus);
      }
    }
    
    if (onBind!=null)
    { 
      onBind.bind(focus);
      onBind.get();
    }
    return focus;
  }
  
  @SuppressWarnings("unchecked")
  protected void updateSessionValue(T val)
  {
    ((SessionState<T>) httpSessionChannel.get().getAttribute(attributeName))
      .value=val;
  }
    
  @Override
  protected void popSubject(HttpServletRequest request)
  { 
    if (request.getSession()==null)
    { 
      sessionChannel.pop();
      return;
    }
    
    if (afterRequest!=null)
    { afterRequest.get();
    }

    if (triggers!=null)
    { 
      @SuppressWarnings("unchecked")
      SessionState<T> sessionState=
        ((SessionState<T>) request.getSession().getAttribute(attributeName));
      
      synchronized (sessionState)
      {
        viewCache.push();
        try
        {
          if (sessionState.triggerStates==null)
          { 
            viewCache.init();
            sessionState.triggerStates=viewCache.get();
          }
          else
          { viewCache.set(sessionState.triggerStates);
          }
      
          viewCache.touch();
      
          for (Binding<?> trigger: triggers)
          { trigger.get();
          }
      
          viewCache.checkpoint();
          sessionState.triggerStates=viewCache.get();
        }
        finally
        { viewCache.pop();
        }
      }
    }
    
    if (onExpire!=null 
        && !request.isRequestedSessionIdValid() 
        && request.getSession()!=null
        )
    {
      @SuppressWarnings("unchecked")
      SessionState<T> sessionState=
        ((SessionState<T>) request.getSession().getAttribute(attributeName));
           
      if (!sessionState.processedExpired)
      {
        onExpire.get();
        sessionState.processedExpired=true;
      }
    }
    sessionChannel.pop();
  }
  
  

  @SuppressWarnings("unchecked")
  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
    throws BindException
  {
      
    HttpSession session=request.getSession(true);
    if (session==null)
    { 
      sessionChannel.push(null);
      return;
    }
    
    SessionState<T> sessionState
      =(SessionState<T>) session.getAttribute(attributeName);
      
    boolean newSession=false;
    if (sessionState==null)
    { 
      // Avoid race condition
      synchronized (session)
      {
        sessionState
          =(SessionState<T>) session.getAttribute(attributeName);
        if (sessionState==null)
        {
          log.fine(getDeclarationInfo()+": Constructing a new session state");
          newSession=true;
          sessionState=new SessionState<T>();
          sessionState.value=constructor.get();
          if (sessionState.value==null)
          { 
            log.warning(getDeclarationInfo()
              +": Constructor returned null for "
                +constructor.getReflector().getTypeURI());
          }
          session.setAttribute(attributeName,sessionState);
          if (debug)
          { 
            log.fine
              ("New sessionValue created: "
              +sessionState.value+"("+reflector.getTypeURI()+")"
              +" in http session "+session.getId()
              );
          }
          
        }
        else
        {
          if (debug)
          { 
            log.fine
              ("Race condition averted for sessionValue "
              +sessionState.value+"("+reflector.getTypeURI()+")"
              +" in http session "+session.getId()
              );
          }
        }
      }  
    }
    sessionChannel.push(sessionState.value);  
    
    if (onConstruct!=null && newSession)
    { onConstruct.get();
    }
    if (onEstablish!=null 
        && !session.isNew()
        && !sessionState.processedEstablished
        )
    { 
      synchronized (sessionState)
      {
        if (!sessionState.processedEstablished)
        {
          onEstablish.get();
          sessionState.processedEstablished=true;
        }
      }
    }
        
    if (onRequest!=null)
    { onRequest.get();
    }
      
    
  }
  
  @Override
  public void destroy()
  {
    if (onDestroy!=null)
    { onDestroy.get();
    }
    super.destroy();
  }


}

class SessionState<T>
{ 
  T value;
  volatile boolean processedEstablished;
  volatile boolean processedExpired;
  boolean updated;
  volatile ViewState<?>[] triggerStates;
}

