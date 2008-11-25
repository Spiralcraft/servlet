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
package spiralcraft.servlet.webui.components.rpc;


//import spiralcraft.log.ClassLog;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.CommandMessage;





public class Controller<T>
  extends ControlGroup<T>
{
  //private static final ClassLog log=ClassLog.getInstance(Controller.class);
  
  private static final CommandMessage COMMAND_MESSAGE=new CommandMessage();
    
  @Override
  protected void handlePrepare(ServiceContext context)
  {
    super.handlePrepare(context);
    relayMessage(context,COMMAND_MESSAGE,null);
  }
  
  
  @Override
  public ControllerState<T> createState()
  { return new ControllerState<T>(this);
  }
 
  
  public class ControllerState<X>
    extends ControlGroupState<X>
  {
    public ControllerState(Controller<X> form)
    { super(form);
    }

  }
}
