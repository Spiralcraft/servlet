//
//Copyright (c) 2015 Michael Toth
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

import spiralcraft.app.kit.StateReferenceHandler;
import spiralcraft.common.ContextualException;

public class ServiceRootComponent
  extends Component
{
  
  StateReferenceHandler<ServiceRootComponentState> stateRef
    =new StateReferenceHandler<>(this,ServiceRootComponentState.class);
  
  { alwaysRunHandlers=true;
  }
  
  @Override
  protected void addHandlers()
    throws ContextualException
  { 
    addHandler(stateRef);
    super.addHandlers();
  }
  
  @Override
  public ServiceRootComponentState createState()
  { return new ServiceRootComponentState(this);
  }
  
  public void dirty()
  { stateRef.get().dirty=true;
  }
  
}
