//
//Copyright (c) 2008,2011 Michael Toth
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

import java.net.URI;

import javax.servlet.ServletException;

import spiralcraft.app.Dispatcher;
import spiralcraft.textgen.PrepareMessage;

import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.Level;
import spiralcraft.servlet.webui.SaveMessage;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.CommandMessage;
import spiralcraft.servlet.webui.GatherMessage;

import spiralcraft.util.ArrayUtil;

import spiralcraft.textgen.EventContext;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.common.ContextualException;
import spiralcraft.util.Sequence;

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
  
  private static final GatherMessage GATHER_MESSAGE=GatherMessage.INSTANCE;
  private static final CommandMessage COMMAND_MESSAGE=CommandMessage.INSTANCE;
  private static final PrepareMessage PREPARE_MESSAGE=PrepareMessage.INSTANCE;
  private static final SaveMessage SAVE_MESSAGE=SaveMessage.INSTANCE;
  
  private Expression<?> onPost;
  private Channel<Command<?,?,?>> onPostChannel;
  private Binding<Void> onPostX;
  private Binding<Void> onSaveX;
    
  private String clientPostActionName;
  private String resetActionName;
  
  private Channel<ServiceContext> serviceContextChannel;
  private boolean autoSave;
  
  public void setOnPost(Expression<Command<?,?,?>> onPost)
  { this.onPost=onPost;
  }
  
  public void setAutoSave(boolean autoSave)
  { this.autoSave=autoSave;
  }

  public void setOnSave(Binding<Void> onSaveX)
  { 
    this.removeExportContextual(this.onSaveX);
    this.onSaveX=onSaveX;
    this.addExportContextual(this.onSaveX);
  }
  
  public void setOnPostX(Binding<Void> onPostX)
  {
    this.removeExportContextual(this.onPostX);
    this.onPostX=onPostX;
    this.addExportContextual(this.onPostX);
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
    Sequence<Integer> path=context.getState().getPath();
    
    String pathString;
    if (isClearable)
    { pathString=path.format(".");
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
            +getTargetPath().format(".")
            );
        }
        // XXX Need to do this recursively?
        scatter(context);
        
      }
    };
  }

  
  protected abstract boolean wasActioned(ServiceContext context);

  protected String pathToActionName(Sequence<Integer> path)
  { return path.format(".");
  }
  
  /**
   * <p>Create a new Action target for the Acceptor
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  protected Action createAction(Dispatcher context,final boolean isClearable)
  {
    Sequence<Integer> path=context.getState().getPath();
    
    String pathString;
    if (isClearable)
    { pathString=pathToActionName(path);
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
      public void invoke(ServiceContext context)
      { 
        if (debug)
        {
          log.fine
            (getLogPrefix()+": Accept action invoked: "
            +getTargetPath().format(".")
            );
        }

        AcceptorState<T> formState=getState(context);
        formState.saveRequested=false;
        
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
            relayMessage(context,PREPARE_MESSAGE);
          }
          relayMessage(context,GATHER_MESSAGE);
        
          if (!formState.isErrorState())
          { 
            // Don't run commands if any vars have errors
            relayMessage(context,COMMAND_MESSAGE);
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
          
          if (onPostX!=null && !formState.isErrorState())
          { onPostX.get();
          }

          if ( (autoSave || formState.saveRequested) 
                && !formState.isErrorState()
             )
          { save(context);

          }
        }
        
      }
    };
  }
    
  protected void save(Dispatcher context)
  {            
    relayMessage(context,SAVE_MESSAGE);
    if (onSaveX!=null && !getState().isErrorState())
    { onSaveX.get();
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  protected Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  {
    if (onPost!=null)
    { 
      onPostChannel=(Channel) focus.bind(onPost);
      if ( ((Channel<?>) onPostChannel).getContentType()!=Command.class)
      { 
        // Allow for onPost to be an expression
        onPostX=new Binding<Void>((Expression) onPost);
        onPostX.bind(focus);
        onPostChannel=null;
      }
    }
    serviceContextChannel
      =focus.<ServiceContext>findFocus(ServiceContext.FOCUS_URI)
        .getSubject();
    return focus;
    
  }
  
  public Command<Void,Void,Void> saveCommand()
  {
    return new CommandAdapter<Void,Void,Void>()
    {
      { name="save";
      }
          
      @Override
      public void run()
      { 
        getState().saveRequested=true;
      }
  
    };
  }
  
  public void save()
  { getState().saveRequested=true;
  }
  
  public Command<Void,Void,Void> redirectCommand(final String redirectURI)
  {
    return new CommandAdapter<Void,Void,Void>()
    {
      { name="redirect";
      }
          
      @Override
      public void run()
      { 
        try
        { serviceContextChannel.get().redirect(URI.create(redirectURI));
        }
        catch (ServletException x)
        { log.log(Level.WARNING,"Threw exception on redirect",x);
        }
      }
  
    };
  } 
  
  
  @Override
  public AcceptorState<T> createState()
  { return new AcceptorState<T>(this);
  }
  
  @Override
  public AcceptorState<T> getState()
  { return (AcceptorState<T>) super.getState();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <X> AcceptorState<X> getState(Dispatcher context)
  { return (AcceptorState<X>) super.getState(context);
  }
}
