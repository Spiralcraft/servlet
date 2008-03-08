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
import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.QueuedCommand;

public abstract class Editor
  extends ControlGroup<Buffer>
{
  private static final ClassLogger log=new ClassLogger(Editor.class);
  private BufferChannel bufferChannel;

  public Command<Buffer,Void> revertCommand()
  { 
    return new QueuedCommand<Buffer,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void>()
        {
          public void run()
          { getState().getValue().revert();
          }
        }
      );
  }

  public Command<Buffer,Void> saveCommand()
  { 
    return new QueuedCommand<Buffer,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void>()
        {
          public void run()
          { 
            try
            { getState().getValue().save();
            }
            catch (Exception x)
            { 
              x.printStackTrace();
              getState().setError("Error saving");
              getState().setException(x);
            }
          }
        }
      );
  }

//  XXX belongs in TupleEditor
//  
//  public Command<Buffer,Void> deleteCommand()
//  { 
//    return new QueuedCommand<Buffer,Void>
//      (getState()
//      ,new CommandAdapter<BufferTuple,Void>()
//        {
//          @SuppressWarnings("unchecked")
//          public void run()
//          { 
//            try
//            { 
//              getState().getValue().delete();
//            }
//            catch (Exception x)
//            { 
//              getState().setError("Error queuing command");
//              getState().setException(x);
//            }
//          }
//        }
//      );
//  }

  /**
   * Wraps default behavior and provides a BufferChannel that buffers what
   *   comes from the target expression.
   */
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
    { 
      source=(Channel<DataComposite>) parentFocus.getSubject();
      if (source==null)
      {
        log.fine
          ("No source specified, and parent Focus has no subject: "+parentFocus);
      }
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
