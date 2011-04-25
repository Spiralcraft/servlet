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
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.reflect.BeanReflector;

import spiralcraft.servlet.autofilter.spi.RequestFocusFilter;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;

/**
 * <p>Puts a an arbitrary contextual object in the Focus chain and 
 *   provides a WebUI interface based on it. 
 * </p>
 * 
 * @author mike
 *
 */
public class UIFilter<Tcontext>
  extends RequestFocusFilter<Tcontext>
{

  protected Binding<Tcontext> x;
  protected Binding<String> resourceX;
  protected UIService uiServant;
  
  { setUsesRequest(true);
  }
  
  @Override
  public void init(FilterConfig config)
    throws ServletException
  { 
    super.init(config);
    
  }
  
  public void setX(Binding<Tcontext> x)
  { this.x=x;
  }

  
  public void setResourceX(Binding<String> resourceX)
  { this.resourceX=resourceX;
  }
  
  @Override
  protected Tcontext createValue(
    HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException
  { 
    
    
    if (x!=null)
    { return x.get();
    }
    else
    { return null;
    }

  }
  
  @Override
  public void doChain
    (FilterChain chain
    ,HttpServletRequest request
    ,HttpServletResponse response
    )
    throws ServletException,IOException
  { 
    String sourceLoc=null;
    if (resourceX!=null)
    { sourceLoc=resourceX.get();
    }
      
    RootComponent component=null;
    
    if (sourceLoc!=null)
    {

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

        component
          =uiServant.getRootComponent
            (resource, contextAdapter.getRelativePath(request));

      }
      catch (ContextualException x)
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
  
  
  @SuppressWarnings("unchecked")
  @Override
  protected Reflector<Tcontext> resolveReflector(Focus<?> focus)
      throws BindException
  { 

    if (x!=null)
    { 
      x.bind(focus);
      focus=focus.chain(x);
    }

    if (resourceX!=null)
    { resourceX.bind(focus);
    }
    
    uiServant=new UIService(contextAdapter);    
    uiServant.bind(focus);
    
    if (x!=null)
    { return x.getReflector();
    }
    else
    { return (Reflector<Tcontext>) BeanReflector.<Void>getInstance(Void.class);
    }
  }
  
  
}
