//
//Copyright (c) 2020 Michael Toth
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
package spiralcraft.servlet.rpc;

import java.util.HashMap;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.servlet.rpc.kit.AbstractHandler;
import spiralcraft.util.Path;

/**
 * A Handler that dispatches a request to one of multiple handlers based on the
 *   next path segment.
 * 
 * @author mike
 *
 */
public class DispatchHandler
  extends AbstractHandler
  implements Handler
{

  private Handler[] handlers;
  private HashMap<String,Handler> handlerMap;
  private Handler defaultHandler;
  private Binding<?> contextX;
  private ThreadLocalChannel<Object> contextChannel;
  private boolean debug;
  private boolean requireMapping;
  
  public void setDebug(boolean debug)
  { this.debug=debug;
  }

  /**
   * Default handler if no specific handlers are  mapped to the request
   * 
   * @param defaultX
   */
  public void setDefaultHandler(Handler defaultHandler)
  { this.defaultHandler=defaultHandler;
  }

  /**
   * If true, require that a specified path segment map to a named handler or the 
   *   "*" handler. The default handler will only be used to map when no
   *   additional path segment is specified.
   * 
   * defaults to false
   * 
   * @param requireMapping
   */
  public void setRequireMapping(boolean requireMapping)
  { this.requireMapping=requireMapping;
  }
  
  public void setHandlers(Handler[] handlers)
  { 
    this.handlers=handlers;
    handlerMap=new HashMap<String,Handler>();
    for (Handler handler:handlers)
    { handlerMap.put(handler.getName(),handler);
    }
  }
  
  public void setContextX(Binding<?> contextX)
  { this.contextX=contextX;
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Focus<?> bind(Focus<?> chain)
    throws ContextualException
  {
    chain=super.bind(chain);
    if (contextX!=null)
    { 
      try
      { contextX.bind(chain);
      }
      catch (ContextualException x)
      { throw new ContextualException("Error binding dispatch context",getDeclarationInfo(),x);
      }
      contextChannel=new ThreadLocalChannel(contextX.getReflector());
      chain=chain.chain(contextChannel);
    }
    
    if (handlers!=null)
    { 
      for (Handler handler:handlers)
      { handler.bind(chain);
      }
    }

    if (defaultHandler!=null)
    { defaultHandler.bind(chain);
    }
    return chain;
  }
  
  @Override
  public void handle()
  { 
    if (contextX!=null)
    { contextChannel.push(contextX.get());
    }
    try
    { 
      Call c=call.get();
      Path path=c.getNextPath();
      String pathInfo=c.getPathInfo();
      String handlerName=path.size()>1?path.getElement(1):null;
      if (debug)
      { log.fine(getDeclarationInfo()+":  Path: "+path+" pathInfo: "+pathInfo+" handlerName:"+handlerName);
      }
      
      Handler handler=null;
      Call subCall=c;
      if (handlerMap!=null)
      { 
        handler=handlerMap.get(handlerName);
        if (handler==null 
            && handlerName!=null 
            && !handlerName.isEmpty()
            )
        { 
          handler=handlerMap.get("*");
          if (debug)
          { log.fine(getDeclarationInfo()+":  Handler * mapped");
          }
        }
        else
        { 
          if (debug)
          { log.fine(getDeclarationInfo()+":  Handler mapped from "+handlerName);
          }
        }
        
        if (handler!=null)
        { 
          String newPathInfo=pathInfo;
          if (c.getNextPath().size()>1)
          { 
            newPathInfo=pathInfo.substring(c.getNextPath().getElement(1).length());
            if (newPathInfo.startsWith("/"))
            { newPathInfo=newPathInfo.substring(1);
            }

          }
          subCall=new SubCall
            (c.getNextPath().subPath(1)
            , newPathInfo
            , c
            );
        }
        
      }
       
      if (debug)
      { log.fine(getDeclarationInfo()+":  Default Handler "+defaultHandler);
      }
      
      if (handler==null && (!requireMapping || handlerName==null))
      { handler=defaultHandler;
      }
    
      if (handler!=null)
      { 
        if (debug)
        { log.fine(getDeclarationInfo()+":  Handler is "+handler);
        }
        handler.handle(subCall);
      }
      else
      { 
        if (debug)
        { log.fine(getDeclarationInfo()+": No handler");
        }
        c.respond(404,"Handler not found for request");
      }
    }
    finally
    { 
      if (contextX!=null)
      { contextChannel.pop();
      }
    }
  }

}