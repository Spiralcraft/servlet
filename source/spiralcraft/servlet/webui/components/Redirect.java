//
//Copyright (c) 1998,2009 Michael Toth
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
package spiralcraft.servlet.webui.components;



import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;


import spiralcraft.common.ContextualException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.Level;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.text.html.URLDataEncoder;
import spiralcraft.textgen.EventContext;


/**
 * Redirects
 * 
 * @author mike
 *
 */
public class Redirect
  extends Control<Void>
{

  private URI redirectURI;

  private Channel<Boolean> whenChannel;
  private Channel<URI> locationChannel;
  private Expression<Boolean> when;
  private Expression<URI> locationX;
  private String refererParameter;
  private boolean mergeQuery;

  public void setRedirectURI(URI uri)
  { redirectURI=uri;
  }
  
  public void setLocationX(Expression<URI> locationX)
  { this.locationX=locationX;
  }
  
  public void setWhen(Expression<Boolean> when)
  { this.when=when;
  }
  
  public void setRefererParameter(String refererParameter)
  { this.refererParameter=refererParameter;
  }

  public void setMergeQuery(boolean mergeQuery)
  { this.mergeQuery=mergeQuery;
  }
  
  @Override
  public String getVariableName()
  { return null;
  }
  
  @Override
  protected void gather(ServiceContext context)
  {
  }
  
  @Override
  public Focus<?> bind(Focus<?> focus)
    throws ContextualException
  { 
    if (when!=null)
    { whenChannel=focus.bind(when);
    }
    if (locationX!=null)
    { locationChannel=focus.bind(locationX);
    }
    return super.bind(focus);
  }  
  

  private void setupRedirect(ServiceContext context)
    throws ServletException
  {
    String referer=context.getRequest().getRequestURL().toString();
    
    URI refererURI;
    try
    { refererURI=new URL(referer).toURI();
    }
    catch (URISyntaxException x)
    { throw new ServletException(x);
    }
    catch (MalformedURLException x)
    { throw new ServletException(x);
    }
    
    URI redirectURI
      =locationChannel==null?this.redirectURI:locationChannel.get();
    if (redirectURI==null)
    { redirectURI=this.redirectURI;
    }
      
    String redirectScheme=redirectURI.getScheme();
    if (redirectScheme==null)
    { redirectScheme=refererURI.getScheme();
    }
    
    String redirectAuthority=redirectURI.getAuthority();
    if (redirectAuthority==null)
    { redirectAuthority=refererURI.getAuthority();
    }
    
    // If no path was specified, use the request path
    String redirectPath=redirectURI.getPath();
    if (redirectPath==null || redirectPath.length()==0)
    { redirectPath=refererURI.getPath();
    }
    
    String redirectQuery=redirectURI.getRawQuery();
    if (redirectQuery==null)
    { 
      if (mergeQuery)
      { redirectQuery=refererURI.getRawQuery();
      }
    }
    else if (mergeQuery && refererURI.getRawQuery()!=null)
    { redirectQuery=refererURI.getRawQuery()+"&"+redirectQuery;
    }

    if (refererParameter!=null)
    { 
      if (redirectQuery==null || !redirectQuery.contains(refererParameter+"="))
      {
        String refererQuery=refererParameter+"="+URLDataEncoder.encode(referer);
        if (redirectQuery!=null)
        { redirectQuery=redirectQuery+"&"+refererQuery;
        }
        else
        { redirectQuery=refererQuery;
        }
      }
      else
      {
        if (debug)
        { 
          log.debug
            ("Skipping referer insertion: redirect query ["+redirectQuery
            +"] already contains referer parameter '"+refererParameter+"'"
            );
        }
      }
    }

    
    try
    {
      URI redirect
        =new URI
          (redirectScheme
          +"://"
          +redirectAuthority
          +(redirectPath!=null?redirectPath:"")
          +(redirectQuery!=null?"?"+redirectQuery:"")
          );

      if (redirect.equals(refererURI))
      { 
        if (debug)
        { log.fine("Skipping self-redirect to "+redirect);
        }
      }
      else
      {   
        if (debug)
        { log.fine("Setting up redirect to "+redirect);
        }         
        context.redirect(redirect);
      }
    }
    catch (URISyntaxException x)
    { throw new ServletException(x);
    }
  }
  
  @Override
  protected void scatter(ServiceContext context)
  { 
    Boolean val=whenChannel!=null?whenChannel.get():Boolean.TRUE;
    if (Boolean.TRUE.equals(val))
    {
      try 
      { setupRedirect(context);
      }
      catch (ServletException x)
      { log.log(Level.WARNING,getLogPrefix(context),x);
      }
    }
   
  }  
    
  @Override
  public void render(EventContext context)
    throws IOException
  {

    Boolean val=whenChannel!=null?whenChannel.get():Boolean.TRUE;
    if (val!=null && val)
    {
      log.fine(getLogPrefix(context)+":Redirect on render");
      try
      { setupRedirect((ServiceContext) context);
      }
      catch (ServletException x)
      { log.log(Level.WARNING,getLogPrefix(context),x);
      }
        
    }
    else
    { super.render(context);
    }
  }

}



