//
//Copyright (c) 2009 Michael Toth
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
package spiralcraft.servlet.webui;

import java.io.IOException;
import java.net.URI;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;

import spiralcraft.servlet.HttpFocus;
import spiralcraft.servlet.autofilter.spi.RequestFocusFilter;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;

/**
 * <p>Maps an expression to a WebUI RootComponent that will be presented
 *   by the UIServlet
 * </p>
 * 
 * @author mike
 *
 */
public class UIFilter<Tcontext>
  extends RequestFocusFilter<RootComponent>
{

  // TODO: Inject an HTTP focus if needed
  // TODO: Provide for a "when" expression so filter doesn't do processing
  //          all the time
  // TODO: Provide for exporting an arbitrary set of objects or performing
  //          actions when a component path is resolved.

  protected Binding<Tcontext> x;
  protected ThreadLocalChannel<Tcontext> xLocal;
  protected Binding<Boolean> whenX;
  protected Binding<String> resourceX;
  protected UIService uiServant;
//  private Channel<RootComponent> dynamicComponent;
  protected HttpFocus<?> httpFocus;
  
  public void setX(Binding<Tcontext> x)
  { this.x=x;
  }
  
  public void setWhenX(Binding<Boolean> whenX)
  { this.whenX=whenX;
  }
  
  public void setResourceX(Binding<String> resourceX)
  { this.resourceX=resourceX;
  }
  
  @Override
  protected RootComponent createValue(
    HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException
  {
    
//    if (dynamicComponent!=null)
//    {
//      RootComponent component
//        =dynamicComponent.get();
//      if (component!=null)
//      { return component;
//      }
//    }
    
    if (httpFocus!=null)
    { httpFocus.push(config.getServletContext(),request,response);
    }
    
    if (x!=null)
    { xLocal.push();
    }


    
    try
    {
      if (whenX!=null && !(Boolean.TRUE.equals(whenX.get())))
      { 
        if (debug)
        { log.fine("whenX is false: "+whenX.getText());
        }
        return null;
      }

      String sourceLoc=null;
      if (resourceX!=null)
      { sourceLoc=resourceX.get();
      }
      
      if (sourceLoc==null)
      { return null;
      }

      URI uri=URI.create(sourceLoc);
      Resource resource;
      try
      {
        if (!uri.isAbsolute())
        { resource=contextAdapter.getResource(sourceLoc);
        }
        else
        { resource=Resolver.getInstance().resolve(uri);
        }

        if (!resource.exists())
        { 
          throw new ServletException
            ("Resource "+resource.getURI()+" does not exist");
        }

        RootComponent component
          =uiServant.getRootComponent
            (resource, contextAdapter.getRelativePath(request));

        return component;
      }
      catch (MarkupException x)
      { 
        throw new ServletException
          ("Error loading webui Component ["+uri+"]:"+x,x);
      }    
      catch (IOException x)
      {
        throw new ServletException
          ("Error loading webui Component for ["+uri+"]:"+x,x);
      }
    }
    catch (ServletException x)
    { 
      pop();
      throw x;
    }
    catch (RuntimeException x)
    { 
      pop();
      throw x;
    }
  }
  
  public void doChain
    (FilterChain chain
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
    throws ServletException,IOException
  { 
    RootComponent component=channel.get();
    if (component!=null)
    { 
      uiServant.service
        (component
        ,request.getServletPath()
        ,config.getServletContext()
        ,request
        ,response
        );
    }
    else
    { super.doChain(chain,request,response);
    }
  }
  
  protected void releaseValue()
  { pop();
  }
  
  protected void pop()
  {
    if (xLocal!=null)
    { xLocal.pop();
    }
    if (httpFocus!=null)
    { httpFocus.pop();
    }
  }

  @Override
  public Focus<?> bindImports(Focus<?> focus)
    throws BindException
  {

    if (focus.findFocus
        (URI.create("class:/javax/servlet/http/HttpServletRequest")
        ) ==null
       )
    { 
      httpFocus=new HttpFocus<Void>(focus);
      focus=httpFocus;
    }
    
    if (x!=null)
    { 
      x.bind(focus);
      xLocal=new ThreadLocalChannel<Tcontext>(x,true);
      focus=focus.chain(x);
    }

    if (whenX!=null)
    { whenX.bind(focus);
    }

    if (resourceX!=null)
    { resourceX.bind(focus);
    }
    
    uiServant=new UIService(contextAdapter);    
    uiServant.bind(focus);
    return focus;
  }
  
  @Override
  protected Reflector<RootComponent> resolveReflector(
    Focus<?> parentFocus)
      throws BindException
  { 

//    dynamicComponent
//      =RootComponent.findChannel(parentFocus);

    uiServant.bind(parentFocus);

    return BeanReflector.<RootComponent>getInstance(RootComponent.class);
  }
  
  
}
