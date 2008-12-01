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



import java.net.URI;
import java.util.List;

import javax.servlet.ServletException;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.command.CommandBlock;
import spiralcraft.data.DataException;
import spiralcraft.data.Type;
import spiralcraft.data.lang.DataReflector;

import spiralcraft.data.session.DataSession;
import spiralcraft.data.session.Buffer;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLog;

import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.SaveMessage;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;
import spiralcraft.textgen.MessageHandler;
import spiralcraft.util.ArrayUtil;

/**
 * Provides common functionality for Editors
 * 
 * @author mike
 *
 */
public abstract class EditorBase<Tbuffer extends Buffer>
  extends ControlGroup<Tbuffer>
{
  private static final ClassLog log
    =ClassLog.getInstance(EditorBase.class);

  private static final SaveMessage SAVE_MESSAGE=new SaveMessage();
  
  protected Channel<Buffer> bufferChannel;
  
  private Type<?> type;
  private Channel<DataSession> sessionChannel;

  protected String newActionName;
  
  protected Assignment<?>[] fixedAssignments;
  protected Assignment<?>[] initialAssignments;
  protected Assignment<?>[] defaultAssignments;
  protected Assignment<?>[] newAssignments;
  protected Assignment<?>[] publishedAssignments;
  protected RequestBinding<?>[] requestBindings;
  protected RequestBinding<?>[] redirectBindings;

  
  private URI redirectOnSaveURI;
  private String redirectOnSaveParameter;
  
  protected boolean autoCreate;
  protected boolean retain;

  {
    addHandler
      (new MessageHandler()
      {

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(EventContext context, Message message,
            boolean postOrder)
        { 
          EditorState state=(EditorState) context.getState();
          if (!postOrder && message.getType()==SaveMessage.TYPE)
          {
            try
            { 
              save();
              Command<?,?> postSaveCommand=state.getPostSaveCommand();
              if (postSaveCommand!=null)
              { 
                if (!state.isErrorState())
                { 
                  if (debug)
                  { log.fine("Executing "+postSaveCommand);
                  }
                  postSaveCommand.execute();
                  if (postSaveCommand.getException()!=null)
                  { handleException(context,postSaveCommand.getException());
                  }
                  
                }
                state.setPostSaveCommand(null);
              }
            }
            catch (DataException x)
            { handleException(context,x);
            }
          }
          
          if (postOrder 
              && state.isRedirect()
              )
          { 
            URI uri=state.getRedirectURI();
            state.setRedirect(false);
            state.setRedirectURI(null);
            handleRedirect((ServiceContext) context,uri);
          }          
        }
      });
  }  

  private void applyRedirectBindings(ServiceContext context)
  {
    if (redirectBindings!=null)
    {
      for (RequestBinding<?> binding: redirectBindings)
      { 
        if (debug)
        { log.fine("Applying redirectBinding "+binding.getName());
        }
        binding.publish(context);
      }
    }
  }
  
  private void handleRedirect(ServiceContext context,URI specificRedirectURI)
  {
    URI redirectURI=redirectOnSaveURI;
            
    if (specificRedirectURI==null)
    {
      if (redirectOnSaveParameter!=null)
      {
        String value
          =context.getQuery()
            .getOne(redirectOnSaveParameter);
        if (value!=null)
        { redirectURI=URI.create(value);
        }
      }
    }
    else
    { redirectURI=specificRedirectURI;
    }
            
    // Handle redirect once the save tree has completed with no
    //   errors
    if (redirectURI!=null)
    { 
      if (debug)
      { log.fine("Redirecting to "+redirectURI);
      }

      // Don't mix up parameters intended for the current page. 
      context.clearParameters();
      
      applyRedirectBindings(context);
      
      try
      { context.redirect(redirectURI);
      }
      catch (ServletException x)
      { handleException(context,x);
      }
    }
  }
  
  
  /**
   * 
   * @param uri A static URI to redirect to when a save has been
   *   performed without error.
   */
  public void setRedirectOnSaveURI(URI uri)
  { this.redirectOnSaveURI=uri;
  }
  
  
  /**
   * @param param The query string parameter which holds the URI to
   *   redirect to when the Editor saves successfully.
   */
  public void setRedirectOnSaveParameter(String param)
  { this.redirectOnSaveParameter=param;
  }
  
  protected abstract void save()
    throws DataException;
  
  protected void setupSession(Focus<?> parentFocus)
  {
    Focus<DataSession> sessionFocus
      =parentFocus.<DataSession>findFocus(DataSession.FOCUS_URI);
    if (sessionFocus!=null)
    { sessionChannel=sessionFocus.getSubject();
    }
  }
  
  protected boolean writeToModel(Tbuffer buffer)
  { return bufferChannel.set(buffer);
  }
  
  protected DataSession getDataSession()
  { return sessionChannel.get();
  }
  
  
  public Type<?> getType()
  { return type;
  }
  
  public void setupType()
    throws BindException
  {
    Type<?> newType
      =((DataReflector<?>) bufferChannel.getReflector()).getType();

    if (type!=null)
    { 
      // Subtype expressed in config
      if (!newType.isAssignableFrom(type))
      { 
        throw new BindException
          ("target type "+newType.getURI()
          +" is not assignable from configured type "+type.getURI()
          );
      
      }
    }
    else
    { type=newType;
    }
    
  }
  
  /**
   * The action name to use for the "new" action, which creates a new
   *   buffer.
   * 
   * @param name
   */
  public void setNewActionName(String name)
  { newActionName=name;
  }
  
  /**
   * The Editor will create a new Buffer if the source provides a null
   *   original value and buffers are not being retained
   */
  public void setAutoCreate(boolean val)
  { autoCreate=val;
    
  }
  
  /**
   * Retain any original value if the source provides a null value
   */
  public void setRetain(boolean val)
  { retain=val;
  }
  

  

  
  /**
   * New Assignments get executed when a buffer is new (ie. has no original) 
   *   and is not yet dirty.
   * 
   * @param assignments
   */
  public void setNewAssignments(Assignment<?>[] assignments)
  { newAssignments=assignments;
  }
  
  /**
   * Initial Assignments get executed when a buffer is not yet dirty.
   * 
   * @param assignments
   */
  public void setInitialAssignments(Assignment<?>[] assignments)
  { initialAssignments=assignments;
  }

  /**
   * <p>Default Assignments get executed immediately before storing, if
   *   the Tuple is dirty already, and the existing field data is null.
   *   
   * @param assignments
   */
  public void setDefaultAssignments(Assignment<?>[] assignments)
  { defaultAssignments=assignments;
  }

  /**
   * <p>Fixed Assignments get executed immediately before storing, if the
   *   Tuple is dirty already, overwriting any existing field data.
   * </p>
   * 
   * @param assignments
   */
  public void setFixedAssignments(Assignment<?>[] assignments)
  { fixedAssignments=assignments;
  }

  /**
   * <p>Published assignments get executed on the Prepare message, which 
   *   occurs before rendering. This permits publishing of data to
   *   containing contexts. 
   * </p>
   * 
   * @param assignments
   */
  public void setPublishedAssignments(Assignment<?>[] assignments)
  { publishedAssignments=assignments;
  }

  /**
   * <p>RequestBindings are applied to the buffer on every request
   * </p>
   * 
   * @param bindings
   */
  public void setRequestBindings(RequestBinding<?>[] bindings)
  { requestBindings=bindings;
  }

  /**
   * <p>RedirectBindings are published to any supplied redirectURI, immediately
   *   before the redirect takes place.
   * </p>
   * 
   * @param bindings
   */
  public void setRedirectBindings(RequestBinding<?>[] bindings)
  { 
    if (bindings!=null)
    { 
      for (RequestBinding<?> binding: bindings)
      { binding.setPublish(true);
      }
    }
    redirectBindings=bindings;
  }
  
  public boolean isDirty()
  { 
    ControlGroupState<Tbuffer> state=getState();
    return state.getValue()!=null && state.getValue().isDirty();
  }
                     
  public Command<Tbuffer,Void> redirectCommand(final String redirectURI)
  {
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        {
          { name="redirect";
          }
          
          @Override
          public void run()
          { 
            getState().setRedirect(true);
            getState().setRedirectURI(URI.create(redirectURI));
          }
        }
      );
  }
  
  public Command<Tbuffer,Void> redirectCommand()
  {
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        {
          { name="redirect";
          }

          @Override
          public void run()
          { getState().setRedirect(true);
          }
        }
      );
  }
  
  public Command<Tbuffer,Void> revertCommand()
  { 
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        {
          { name="revert";
          }

          @Override
          public void run()
          { getState().getValue().revert();
          }
        }
      );
  }

  public Command<Tbuffer,Void> revertCommand(final Command<?,?> chainedCommand)
  { 
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        {
          { name="revert";
          }

          @Override
          public void run()
          { 
            getState().getValue().revert();
            chainedCommand.execute();
          }
        }
      );
  }

  /**
   * A Command that saves the buffer tree starting with this buffer.
   * 
   * @return the Save command
   */
  public Command<Tbuffer,Void> saveCommand()
  {     
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        { 
          { name="save";
          }
          
          @Override
          public void run()
          { 
            if (!getState().isErrorState())
            { getState().queueMessage(SAVE_MESSAGE);
            }
            
          }
        }
      );
  }
  
  @SuppressWarnings("unchecked") // Command block doesn't care about types
  public Command<Tbuffer,Void> saveCommand
    (final List<Command> postSaveCommandList)
  {
    
    return saveCommand(new CommandBlock(postSaveCommandList));
  }
      
  public Command<Tbuffer,Void> saveCommand(final Command<?,?> postSaveCommand)
  { 
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        { 
          { name="save";
          }

          @Override
          public void run()
          { 
            if (!getState().isErrorState())
            {
              getState().queueMessage(SAVE_MESSAGE);
              getState().setPostSaveCommand(postSaveCommand);
            }
          }
        }
      );
  }
  
  /**
   * <p>Clears the Editor to accomodate a new Tuple
   * </p>
   * 
   * @return A new Command
   */
  public Command<Tbuffer,Void> clearCommand()
  { 

    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        { 
          { name="clear";
          }
          
          @Override
          public void run()
          { getState().setValue(null);
          }
        }
      );
  }



  public Command<Tbuffer,Void> newCommand()
  {
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        { 
          { name="new";
          }

          @Override
          public void run()
          { 
            try
            { newBuffer();
            }
            catch (DataException x)
            { setException(x);
            }
          }
        }
      );
  }






  
  /**
   * Create a new buffer
   */
  @SuppressWarnings("unchecked")
  protected void newBuffer()
    throws DataException
  {
    getState().setValue
    ((Tbuffer) sessionChannel.get().newBuffer(type)
    );
    writeToModel(getState().getValue());
  }
  
  

 
  @Override
  protected void handleInitialize(ServiceContext context)
  {
    super.handleInitialize(context);
    if (newActionName!=null)
    { context.registerAction(createNewAction(context));
    }
    
  }
  
  @Override
  protected void handlePrepare(ServiceContext context)
  { 
// non-clearable action in init is all we need    
//    if (newActionName!=null)
//    { context.registerAction(createNewAction(context), newActionName);
//    }

    if (redirectOnSaveParameter!=null && context.getQuery()!=null)
    {
      String redirectURI=context.getQuery().getOne(redirectOnSaveParameter);
      if (redirectURI!=null)
      { context.setActionParameter(redirectOnSaveParameter,redirectURI);
      }
      
    }
    
    super.handlePrepare(context);
  }
   

  @SuppressWarnings("unchecked")
  @Override
  protected void scatter(ServiceContext context)
  { 
    EditorState<Tbuffer> state=(EditorState<Tbuffer>) context.getState();
    Tbuffer lastBuffer=state.getValue();
   
    super.scatter(context);
    if (state.getValue()==null)
    { 
      // Deal with new value being null
      if (lastBuffer==null)
      { 
        if (autoCreate)
        {
          // Current value was null, and newly scattered value was null
          try
          { newBuffer();
          }
          catch (DataException x)
          { handleException(context,x);
          }
          
          if (debug)
          { log.fine("Created new buffer "+state.getValue());
          }
        }
        else
        {
          if (debug)
          { log.fine("Buffer remains null (autoCreate==false)");
          }
        }
      }
      else if (lastBuffer.getOriginal()==null)
      { 
        if (debug)
        { log.fine("New buffer is sticky "+lastBuffer);
        }
        state.setValue(lastBuffer);
      }
      else
      {
        if (retain)
        {
          if (debug)
          { log.fine("Retaining buffer "+lastBuffer);
          }
          state.setValue(lastBuffer);
        }
        else if (autoCreate)
        {
          try
          {
            newBuffer();
            if (debug)
            { 
              log.fine
                ("Created new buffer to replace last buffer: new="
                +state.getValue()
                );
            }
          }
          catch (DataException x)
          { handleException(context,x);
          }
        }
        else
        {
          if (debug)
          { 
            log.fine
              ("Replacing last buffer with null " 
              +"(autoCreate==false && retain==false)"
              );
          }
        }
        
      }
    }
    
  }
  
  /**
   * <p>Create a new Action target for the Form post
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  protected Action createNewAction(EventContext context)
  {
    return new Action(newActionName,context.getState().getPath())
    {

      { clearable=false;
      }
      
      @Override
      public void invoke(ServiceContext context)
      { 
        if (debug)
        {
          log.fine
            ("Editor: Action invoked: "+getName()+"@"
            +ArrayUtil.format(getTargetPath(),".",null)
            );
        }
        try
        { newBuffer();
        }
        catch (DataException x)
        { handleException(context,x);
        }
        
        
      }
    };
  }  
  

  @Override
  public EditorState<Tbuffer> createState()
  {
    return new EditorState<Tbuffer>(this);
  }  
  
  @Override
  public EditorState<Tbuffer> getState()
  { return (EditorState<Tbuffer>) super.getState();
  }
}

class EditorState<T extends Buffer>
  extends ControlGroupState<T>
{
  
  private boolean redirect;
  private URI redirectURI;
  private Command<?,?> postSaveCommand;
  
  public EditorState(EditorBase<T> editor)
  { super(editor);
  }
  
  public void setRedirect(boolean redirect)
  { this.redirect=redirect;
  }
    
  public boolean isRedirect()
  { return redirect;
  }
  
  public void setRedirectURI(URI redirectURI)
  { this.redirectURI=redirectURI;
  }
  
  public URI getRedirectURI()
  { return redirectURI;
  }
  
  public Command<?,?> getPostSaveCommand()
  { return postSaveCommand;
  }
  
  public void setPostSaveCommand(Command<?,?> postSaveCommand)
  { this.postSaveCommand=postSaveCommand;
  }

}
