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
package spiralcraft.servlet.webui.components;



import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.servlet.ServletException;


import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;

import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.text.html.URLDataEncoder;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;


/**
 * Redirects
 * 
 * @author mike
 *
 */
public class Redirect
  extends Component
{
  private static final ClassLog log
    =ClassLog.getInstance(Guard.class);

  private URI redirectURI;

  private Channel<Boolean> whenChannel;
  private Expression<Boolean> when;
  private String refererParameter;

  public void setRedirectURI(URI uri)
  { redirectURI=uri;
  }
  
  public void setWhen(Expression<Boolean> when)
  { this.when=when;
  }
  
  public void setRefererParameter(String refererParameter)
  { this.refererParameter=refererParameter;
  }

  @Override
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    Focus<?> parentFocus=getParent().getFocus();
    if (when!=null)
    { whenChannel=parentFocus.bind(when);
    }
    super.bind(childUnits);
  }  
  

  private void setupRedirect(ServiceContext context)
    throws ServletException
  {
    String referer=context.getRequest().getRequestURL().toString();
    String refererQuery="";
    if (refererParameter!=null)
    { refererQuery="?"+refererParameter+"="+URLDataEncoder.encode(referer);
    }
    URI redirect
      =URI.create
        (redirectURI.getPath()+refererQuery);
    if (debug)
    { log.fine("Setting up redirect to "+redirect);
    }
    context.redirect(redirect);
  }
  
  @Override
  protected void handlePrepare(ServiceContext context)
  { 
    super.handlePrepare(context);
    Boolean val=whenChannel!=null?whenChannel.get():true;
    if (val!=null && val)
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

    Boolean val=whenChannel!=null?whenChannel.get():true;
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



