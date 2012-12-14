//
//Copyright (c) 1998,2011 Michael Toth
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
package spiralcraft.servlet.webui.components.html;


import java.io.IOException;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.servlet.webui.components.html.kit.HtmlContainer;

/**
 * Provides client-side javascript support for HTML components
 *  
 * @author mike
 *
 */
public class JSClient
  implements Contextual
{

  private HtmlContainer serverPeer;
  
  @Override
  public Focus<?> bind(Focus<?> focusChain)
    throws ContextualException
  {
    serverPeer=LangUtil.findInstance(HtmlContainer.class,focusChain);
    return focusChain;
  }

  public String notifyJS(String event,String data) 
    throws IOException
  { 
    StringBuilder out=new StringBuilder();
    out.append("$SC('");
    out.append(serverPeer.getState().getId());
    out.append("').events.");
    out.append(event);
    out.append(".notify(");
    out.append(data);
    out.append(")");
    return out.toString();
  }
  
  public String listenJS(String event,String peerRef,String fnRef) 
    throws IOException
  { 
    StringBuilder out=new StringBuilder();
    out.append("$SC('");
    out.append(serverPeer.getState().getId());
    out.append("').events.");
    out.append(event);
    out.append(".listen(");
    out.append(peerRef);
    out.append(",");
    out.append(fnRef);
    out.append(")");
    return out.toString();
  }
  
}
