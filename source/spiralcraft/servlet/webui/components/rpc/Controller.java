//
//Copyright (c) 2008,2008 Michael Toth
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
import spiralcraft.servlet.webui.components.Acceptor;

public class Controller<T>
  extends Acceptor<T>
{
  //private static final ClassLog log=ClassLog.getInstance(Controller.class);
   


  
  @Override
  /**
   * Called within the invocation of an actual action
   */
  protected boolean wasActioned(ServiceContext context)
  { return actionedWhen==null || Boolean.TRUE.equals(actionedWhen.get());
  }
  
  
  
}
    

