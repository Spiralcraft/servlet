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
import java.net.URISyntaxException;

import javax.servlet.ServletException;

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.kit.AbstractMessageHandler;
import spiralcraft.textgen.PrepareMessage;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.webui.RequestMessage;
import spiralcraft.servlet.webui.RevertMessage;
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
  private boolean reloadAfterAction;
  
  protected boolean autoPost;
  protected Binding<Boolean> actionedWhen;
  
  protected String staticActionLink;
  protected String staticResetLink;
  
  
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
  
  /**
   * Force the client to redirect back to this page after an action
   *   is performed so the reload button does not resubmit the form.
   * 
   * @param reloadAfterAction
   */
  public void setReloadAfterAction(boolean reloadAfterAction)
  { this.reloadAfterAction=reloadAfterAction;
  }
  
  
  /**
   * An Expression which determines whether or not this controller
   *   has been actioned.
   * 
   * @param actionedWhen
   */
  public void setActionedWhen(Binding<Boolean> actionedWhen)
  {
    this.removeExportContextual(this.actionedWhen);
    this.actionedWhen=actionedWhen;
    this.addExportContextual(this.actionedWhen);
  }
    
  /**
   * <p>Automatically run the "post" action before the "prepare" stage,
   *   regardless of whether or not the associated action appears in the 
   *   request line.
   * </p>
   * 
   * <p>Useful to trigger data updates and refreshes on each 
   *   page view.
   * </p>
   * 
   * @param autoPost
   */
  public void setAutoPost(boolean autoPost)
  { this.autoPost=autoPost;
  }
  
  @Override
  protected void handleInitialize(ServiceContext context)
  { 
    if (resetActionName!=null)
    { staticResetLink=context.registerAction(createResetAction(context,false));
    }
    if (clientPostActionName!=null)
    { staticActionLink=context.registerAction(createAction(context,false));
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
        formState.revertRequested=false;
        
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
          
          if (formState.revertRequested)
          { revert(context);
          }
          
          if (reloadAfterAction && context.getRedirectURI()==null)
          { 
            try
            { context.redirect(context.getAbsoluteBackLink());
            }
            catch (URISyntaxException x)
            { log.log(Level.WARNING,"Could not redirect to "+context.getAbsoluteBackLink());
            }
            catch (ServletException x)
            { log.log(Level.WARNING,"Could not redirect to "+context.getAbsoluteBackLink(),x);
            }
          }
        }
        
      }
    };
  }
    
  protected void revert(Dispatcher context)
  { relayMessage(context,RevertMessage.INSTANCE);
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
  
  public VariableMap getForm()
  { return serviceContextChannel.get().getForm();
  }
  
  public void save()
  { getState().saveRequested=true;
  }

  public void revert()
  { getState().revertRequested=true;
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
  protected void addHandlers()
    throws ContextualException
  {
    // Add a handler to invoke the automatic post action after all the
    //   subcomponents have processed the RequestMessage
    addHandler
      (new AbstractMessageHandler()
      {
        @Override
        public void doHandler(Dispatcher context, Message message,
            MessageHandlerChain next)
        { 
          next.handleMessage(context,message);
          if (autoPost 
                && message.getType()==RequestMessage.TYPE
                && (actionedWhen==null 
                    || Boolean.TRUE.equals(actionedWhen.get())
                   )
                )
          { createAction(context,false).invoke((ServiceContext) context);
          }
        }
      });
    super.addHandlers();
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
