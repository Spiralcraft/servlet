//
//Copyright (c) 2013 Michael Toth
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

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.PublicLocator;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.UIContext;
import spiralcraft.servlet.webui.kit.PortSession;

/**
 * A fascade which provides the markup API client with access to the state of 
 *   the UI during the servicing of an HTTP request or other message.
 *   
 * @author mike
 *
 */
public class UI
  implements UIContext
{
  
  private ServiceContext sc()
  { return ServiceContext.get();
  }
  
  @Override
  public VariableMap getPost()
  { return sc().getPost();
  }
  
  @Override
  public VariableMap getQuery()
  { return sc().getQuery();
  }

  @Override
  public void redirect(
    String uriStr)
    throws URISyntaxException,
    ServletException
  { sc().redirect(uriStr);
  }

  @Override
  public void redirect(
    URI rawURI)
    throws ServletException
  { sc().redirect(rawURI);
  }

  @Override
  public String getAbsoluteBackLink()
  { return sc().getAbsoluteBackLink();
  }

  @Override
  public PublicLocator getPublicLocator()
  { return sc().getPublicLocator();
  }

  @Override
  public String getDataEncodedAbsoluteBackLink()
  { return sc().getDataEncodedAbsoluteBackLink();
  }

  @Override
  public String getAsyncURL()
  { return sc().getAsyncURL();
  }

  @Override
  public HttpServletResponse getResponse()
  { return sc().getResponse();
  }

  @Override
  public HttpServletRequest getRequest()
  { return sc().getRequest();
  }

  @Override
  public PortSession getPortSession()
  { return sc().getPortSession();
  }
  
  
}
