//
//Copyright (c) 2009 Michael Toth
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
package spiralcraft.servlet.webui.components.kit;

import spiralcraft.command.Command;
import spiralcraft.servlet.webui.ControlState;

/**
 * Holds state for a CommandControl
 * 
 * @author mike
 *
 */
public class ActionControlState<Tcontext,Tresult>
  extends ControlState<Command<?,Tcontext,Tresult>>
{
  public ActionControlState(AbstractActionControl<Tcontext,Tresult> comp)
  { super(comp);
  }
  
  
}