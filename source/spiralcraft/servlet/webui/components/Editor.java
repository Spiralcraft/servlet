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
package spiralcraft.servlet.webui.components;


import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.data.DataComposite;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.ControlGroup;

public class Editor
  extends ControlGroup<Buffer>
{
  private static final ClassLogger log=new ClassLogger(Editor.class);

  public Command<Buffer,Void> revertCommand()
  { 
    return new CommandAdapter<Buffer,Void>()
    {
      public void run()
      { getState().getValue().revert();
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Channel<Buffer> bind
    (Focus<?> parentFocus)
      throws BindException
  { 
    log.fine("Editor.bind() "+parentFocus);
    Channel<?> source=(Channel<DataComposite>) 
      super.bind(parentFocus);
    
    
    if (source==null)
    { source=(Channel<DataComposite>) parentFocus.getSubject();
    }

    if (((DataReflector) source.getReflector()).getType()
        instanceof BufferType
       )
    { 
      log.fine("Already buffering "+source.getReflector());
      // Can't buffer more than once
      return (Channel<Buffer>) source;
    }
    
    if (source.getReflector() 
          instanceof DataReflector
        )
    { 
      log.fine("Buffering "+source.getReflector());
      return new BufferChannel
        ((Focus<DataComposite>) parentFocus
        ,(Channel<DataComposite>) source
        );
    }
    else
    { throw new BindException
        ("Not a DataReflector "
          +parentFocus.getSubject().getReflector()
        );
          
    }
    
  }
 
  
}
