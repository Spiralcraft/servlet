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
package spiralcraft.servlet.webui.components.html.kit;

import java.net.URI;

import spiralcraft.app.State;
import spiralcraft.app.kit.StateReferenceHandler;
import spiralcraft.common.ContextualException;

import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.html.JSClient;
import spiralcraft.servlet.webui.components.html.PeerJSTag;

/**
 * <p>A simple component that has a representation within an HTML document
 * </p>
 *  
 * @author mike
 *
 */
public class HtmlContainer
  extends Component
{
  
  protected JSClient jsClient;
  protected PeerJSTag peerJSTag;
  
  
  private StateReferenceHandler<ComponentState> stateRef
    =new StateReferenceHandler<ComponentState>(ComponentState.class);
  
  { 
    addSelfFacet=true;
  }
  
  public void setPeerJSTag(PeerJSTag scriptTag)
  { 
    if (jsClient==null)
    { jsClient=new JSClient();
    }
    if (scriptTag!=peerJSTag)
    { peerJSTag=scriptTag;
    }
  }
  
  public PeerJSTag getPeerJSTag()
  {
    if (peerJSTag==null)
    { setPeerJSTag(new PeerJSTag());
    }
    return peerJSTag;
  }
  
  @Override
  @SuppressWarnings({"unchecked","rawtypes"})
  // Not using generic versions
  protected final Focus<?> bindStandard(Focus<?> focus) 
    throws ContextualException
  {
    
    if (jsClient!=null)
    { this.addSelfContextual(jsClient);
    }
    focus=super.bindStandard(focus);
    return focus;
    
  }  

  public void setJSClient(JSClient jsClient)
  { this.jsClient=jsClient;
  }
  
  
  public ComponentState getState()
  { return stateRef.get();
  }
  
  public JSClient getJSClient()
  { return jsClient;
  }
  
  
  public URI registerPort(String componentId)
  { 
    ComponentState state=getChildState(componentId);
    if (state!=null)
    { 
      return URI.create
        (ServiceContext.get().registerPort(state.getId(),state.getPath()));
    }
    else
    { throw new RuntimeException("Unrecognized port name "+componentId);
    }
  }
 
  @Override
  protected void addHandlers()
    throws ContextualException
  {
    super.addHandlers();
    addHandler(stateRef);
    addHandler(peerJSTag);
  }
  
  private ComponentState getChildState(String id)
  {
    Integer index=getChildIndex(id);
    if (index!=null)
    { 
      State childState=getState().getChild(index);
      if (childState instanceof ComponentState)
      { return ((ComponentState) childState);
      }
    }
    return null;
  }   
}
