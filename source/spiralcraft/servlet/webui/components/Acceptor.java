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
package spiralcraft.servlet.webui.components;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.PrepareMessage;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLog;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.CommandMessage;
import spiralcraft.servlet.webui.GatherMessage;

import spiralcraft.util.ArrayUtil;

import spiralcraft.command.Command;

/**
 * <p>Accepts a user input action and sequences the processing behavior of 
 *   the child Control tree.
 * </p>
 * @author mike
 *
 * @param <T>
 */
public abstract class Acceptor<T>
  extends ControlGroup<T>
{
  private static final ClassLog log=ClassLog.getInstance(Acceptor.class);
  
  private static final GatherMessage GATHER_MESSAGE=new GatherMessage();
  private static final CommandMessage COMMAND_MESSAGE=new CommandMessage();
  private static final PrepareMessage PREPARE_MESSAGE=new PrepareMessage();
  
  private Expression<Command<?,?>> onPost;
  private Channel<Command<?,?>> onPostChannel;
    
  private String clientPostActionName;
  private String resetActionName;

  
  public void setOnPost(Expression<Command<?,?>> onPost)
  { this.onPost=onPost;
  }
  

  
  public void setClientPostActionName(String name)
  { this.clientPostActionName=name;
  }
  
  public void setResetActionName(String name)
  { this.resetActionName=name;
  }
  
  @Override
  public String getVariableName()
  { return null;
  }
  
    
  @Override
  public void handleInitialize(ServiceContext context)
  { 
    if (resetActionName!=null)
    { context.registerAction(createResetAction(context,false));
    }
    if (clientPostActionName!=null)
    { context.registerAction(createAction(context,false));
    }
  }
  
  /**
   * <p>Create a new Action target to reset the acceptor before handling
   *   unsolicited client input.
   * </p>
   * 
   * <p>XXX This calls "scatter" on itself. We should probably find a way to
   *   have this call "scatter" on child components.
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  protected Action createResetAction(EventContext context,final boolean isClearable)
  {
    int[] path=context.getState().getPath();
    
    String pathString;
    if (isClearable)
    { pathString=ArrayUtil.format(path,".",null);
    }
    else
    { pathString=resetActionName;
    }
    
    return new Action
      (pathString
      ,path
      )
    {
      { this.responsive=isClearable;
      }
      @Override
      public void invoke(ServiceContext context)
      { 
        if (debug)
        { 
          log.fine
            (getLogPrefix()+":Reset action invoked: "
            +ArrayUtil.format(getTargetPath(),"/",null)
            );
        }
        // XXX Need to do this recursively?
        scatter(context);
        
      }
    };
  }

  
  protected abstract boolean wasActioned(ServiceContext context);
  
  /**
   * <p>Create a new Action target for the Acceptor
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  protected Action createAction(EventContext context,final boolean isClearable)
  {
    int[] path=context.getState().getPath();
    
    String pathString;
    if (isClearable)
    { pathString=ArrayUtil.format(path,".",null);
    }
    else
    { pathString=clientPostActionName;
    }
    
    return new Action
      (pathString
      ,path
      )
    {
      { this.responsive=isClearable;
      }
      @Override
      @SuppressWarnings("unchecked") // Blind cast
      public void invoke(ServiceContext context)
      { 
        if (debug)
        {
          log.fine
            (getLogPrefix()+": Accept action invoked: "
            +ArrayUtil.format(getTargetPath(),"/",null)
            );
        }

        ControlGroupState<T> formState
          =(ControlGroupState<T>) context.getState();
        
        if (wasActioned(context))
        {


          formState.resetError(); // Always reset the error on a new post
          if (!responsive && !scatterOnRequest)
          { 
            // Make sure we pre-scatter the whole subtree before running the
            //   permanently registered action or we might not be making the
            //   target data model elements available to the child controls.
            //
            // This makes the non-clearable form action forget any intermediate
            //   state from one request to the next
            //   
            scatter(context);
            relayMessage(context,PREPARE_MESSAGE,null);
          }
          relayMessage(context,GATHER_MESSAGE,null);
        
          if (!formState.isErrorState())
          { 
            // Don't run commands if any vars have errors
            relayMessage(context,COMMAND_MESSAGE,null);
          }
          else
          { 
            if (debug)
            { 
              log.fine
                (getLogPrefix()+"Accept: not running commands due to error state: "
                +formState.getException()+" : "
                +ArrayUtil.format(formState.getErrors(),",",null)
                );
            }
          }
          
          if (onPostChannel!=null && !formState.isErrorState())
          { onPostChannel.get().execute();
          }
          
        }
        
      }
    };
  }
    
  
  
  @Override
  public Focus<?> bindExports()
    throws BindException
  {
    if (onPost!=null)
    { onPostChannel=getFocus().bind(onPost);
    }
    return null;
    
  }

}
