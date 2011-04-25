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
package spiralcraft.servlet.webui;

import spiralcraft.app.Message;


public class ActionMessage
  extends Message
{

  public static final Type TYPE=new Type();
  
  { multicast=false;
  }
  
  private final Action action;
  
  @Override
  public Type getType()
  { return TYPE;
  }
  
  public ActionMessage(Action action)
  { this.action=action;
  }
  
  public Action getAction()
  { return action;
  }
  
  @Override
  public String toString()
  { return super.toString()+": "+action;
  }
  
  
}
