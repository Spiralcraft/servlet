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

import spiralcraft.security.auth.LoginEntry;
import spiralcraft.servlet.webui.ControlGroupState;

public class LoginState
  extends ControlGroupState<LoginEntry>
{
  private String referer;
  private boolean actioned;
  
  public LoginState(Login login)
  { super(login);
  }
  
  public void setReferer(String referer)
  { this.referer=referer;
  }
  
  public String getReferer()
  { return referer;
  }
  
  public void setActioned(boolean actioned)
  { this.actioned=actioned;
  }
  
  public boolean getActioned()
  { return actioned;
  }

}