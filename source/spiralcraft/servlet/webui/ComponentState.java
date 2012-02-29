//
//Copyright (c) 2012 Michael Toth
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


import spiralcraft.app.StateFrame;
import spiralcraft.servlet.webui.kit.PortSession;
import spiralcraft.textgen.ElementState;

public class ComponentState
  extends ElementState
{

  private volatile PortSession portSession;
    
  public ComponentState(Component component)
  { super(component.getChildCount());
  }
  
  public synchronized PortSession getPortSession()
  {
    if (portSession==null)
    { 
      portSession=new PortSession();
      portSession.setState(this);
      portSession.setPort(getPath());
    }
    return portSession;
  }
  
  @Override
  public void enterFrame(StateFrame frame)
  { 
    super.enterFrame(frame);
    if (isNewFrame() && portSession!=null)
    { portSession.setFrame(frame);
    }
  }

}
