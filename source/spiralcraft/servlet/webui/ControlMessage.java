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

import spiralcraft.textgen.Message;


public class ControlMessage
  extends Message
{
  
  public enum Op
  { GATHER
  , SCATTER
  , COMMAND
  };
  
  public static final MessageType TYPE=new MessageType();
  
  /**
   * Instructs components to gather their input from the EventContext.
   */
  public static final ControlMessage GATHER_MESSAGE
    =new ControlMessage(Op.GATHER);
  
  /**
   * Instructs components to execute any queued commands
   */
  public static final ControlMessage COMMAND_MESSAGE
    =new ControlMessage(Op.COMMAND);

  /**
   * Instructs components to refresh any data before rendering output
   */
  public static final ControlMessage SCATTER_MESSAGE
    =new ControlMessage(Op.SCATTER);

  { multicast=true;
  }
  
  private final Op op;
  
  public MessageType getType()
  { return TYPE;
  }
  
  public ControlMessage(Op command)
  { this.op=command;
  }
  
  public Op getOp()
  { return op;
  }
  
  
  
}
