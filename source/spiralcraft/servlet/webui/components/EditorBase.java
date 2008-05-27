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

import javax.servlet.ServletException;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.data.DataException;
import spiralcraft.data.Type;
import spiralcraft.data.lang.DataReflector;

import spiralcraft.data.session.DataSession;
import spiralcraft.data.session.Buffer;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLogger;

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
  private static final ClassLogger log
    =ClassLogger.getInstance(EditorBase.class);

  private static final SaveMessage SAVE_MESSAGE=new SaveMessage();
  
  protected Channel<Buffer> bufferChannel;
  
  private Type<?> type;
  private Channel<DataSession> sessionChannel;

  private String newActionName;
  
  protected Assignment<?>[] fixedAssignments;
  protected Assignment<?>[] initialAssignments;
  protected Assignment<?>[] defaultAssignments;
  protected Assignment<?>[] newAssignments;
  protected Assignment<?>[] publishedAssignments;
  protected RequestBinding<?>[] requestBindings;

  
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
          if (!postOrder && message.getType()==SaveMessage.TYPE)
          {
            try
            { save();
            }
            catch (DataException x)
            { EditorBase.this.getState().setException(x);
            }
          }
          
          if (postOrder 
              && message.getType()==SaveMessage.TYPE
              && !((ControlGroupState<Tbuffer>) context.getState()).isErrorState()
              )
          { handleRedirectOnSave((ServiceContext) context);
          }          
        }
      });
  }  

  private void handleRedirectOnSave(ServiceContext context)
  {
    URI redirectURI=redirectOnSaveURI;
            
    if (redirectOnSaveParameter!=null)
    {
      String value
        =context.getQuery()
          .getOne(redirectOnSaveParameter);
      if (value!=null)
      { redirectURI=URI.create(value);
      }
    }
            
    // Handle redirect once the save tree has completed with no
    //   errors
    if (redirectURI!=null
        && !EditorBase.this.getState().isErrorState()
       )
    { 
      if (debug)
      { log.fine("Redirecting to "+redirectURI);
      }

      // Don't mix up parameters intended for the current page. 
      context.clearParameters();
      
      try
      { context.redirect(redirectURI);
      }
      catch (ServletException x)
      { EditorBase.this.getState().setException(x);
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
   * @param uri The query string parameter which holds the URI to
   *   redirect to when the Editor saves successfully.
   */
  public void setRedirectOnSaveParameter(String param)
  { this.redirectOnSaveParameter=param;
  }
  
  protected abstract void save()
    throws DataException;
  
  @SuppressWarnings("unchecked")
  protected void setupSession(Focus<?> parentFocus)
  {
    Focus<DataSession> sessionFocus
      =(Focus<DataSession>) parentFocus.findFocus(DataSession.FOCUS_URI);
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
   * <p>RequestBindings are applied to the buffer on every request, as
   *   long as the value is not null
   * </p>
   * 
   * @param assignments
   */
  public void setRequestBindings(RequestBinding<?>[] bindings)
  { requestBindings=bindings;
  }
  
  public boolean isDirty()
  { 
    ControlGroupState<Tbuffer> state=getState();
    return state.getValue()!=null && state.getValue().isDirty();
  }
                     
  public Command<Tbuffer,Void> revertCommand()
  { 
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        {
          public void run()
          { getState().getValue().revert();
          }
        }
      );
  }

  public Command<Tbuffer,Void> saveCommand()
  {     
    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        { 
          public void run()
          { 
            getState().queueMessage(SAVE_MESSAGE);
          }
        }
      );
  }
  
  /**
   * <p>Saves the referenced Buffer and 
   * </p>  clears the Editor to accomodate a new Tuple
   * 
   * @return
   */
  public Command<Tbuffer,Void> saveAndClearCommand()
  { 

    return new QueuedCommand<Tbuffer,Void>
      (getState()
      ,new CommandAdapter<Tbuffer,Void>()
        { 
          public void run()
          { 
            getState().queueMessage(SAVE_MESSAGE);
            
            // Executes after the message is processed down the chain.
            getState().queueCommand
              (new CommandAdapter<Tbuffer,Void>()
              {
                public void run()
                { 
                  if (!getState().isErrorState())
                  { getState().setValue(null);
                  }
                }
              }
              );
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
          public void run()
          { newBuffer();
          }
        }
      );
  }






  
  /**
   * Create a new buffer
   */
  @SuppressWarnings("unchecked")
  protected void newBuffer()
  {
    try
    {
      getState().setValue
      ((Tbuffer) sessionChannel.get().newBuffer(type)
      );
      writeToModel(getState().getValue());
    }
    catch (DataException x)
    { 
      x.printStackTrace();
      getState().setException(x);
    }
  }
  
  

 
  protected void handleInitialize(ServiceContext context)
  {
    super.handleInitialize(context);
    if (newActionName!=null)
    { context.registerAction(createNewAction(context), newActionName);
    }
    
  }
  
  protected void handlePrepare(ServiceContext context)
  { 
// non-clearable action in init is all we need    
//    if (newActionName!=null)
//    { context.registerAction(createNewAction(context), newActionName);
//    }

    if (redirectOnSaveParameter!=null)
    {
      String redirectURI=context.getQuery().getOne(redirectOnSaveParameter);
      if (redirectURI!=null)
      { context.setActionParameter(redirectOnSaveParameter,redirectURI);
      }
      
    }
    
    super.handlePrepare(context);
  }
   

  @Override
  protected void scatter(ServiceContext context)
  { 
    Tbuffer lastBuffer=getState().getValue();
   
    super.scatter(context);
    if (getState().getValue()==null)
    { 
      // Deal with new value being null
      if (lastBuffer==null)
      { 
        if (autoCreate)
        {
          // Current value was null, and newly scattered value was null
          newBuffer();
          if (debug)
          { log.fine("Created new buffer "+getState().getValue());
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
        getState().setValue(lastBuffer);
      }
      else
      {
        if (retain)
        {
          if (debug)
          { log.fine("Retaining buffer "+lastBuffer);
          }
          getState().setValue(lastBuffer);
        }
        else if (autoCreate)
        {
          newBuffer();
          if (debug)
          { 
            log.fine
              ("Created new buffer to replace last buffer: new="
              +getState().getValue()
              );
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
   * Create a new Action target for the Form post
   * 
   * @param context
   * @return
   */
  protected Action createNewAction(EventContext context)
  {
    return new Action(context.getState().getPath())
    {

      { clearable=false;
      }
      
      @SuppressWarnings("unchecked") // Blind cast
      public void invoke(ServiceContext context)
      { 
        if (debug)
        {
          log.fine
            ("Editor: Action invoked: "
            +ArrayUtil.format(getTargetPath(),"/",null)
            );
        }
        newBuffer();
        
      }
    };
  }  
  

//  public EditorState<Tbuffer> createState()
//  {
//    return new EditorState<Tbuffer>(this);
//  }  
  
  
}

//class EditorState<T extends Buffer>
//  extends ControlGroupState<T>
//{
//  
//  public EditorState(EditorBase<T> editor)
//  { super(editor);
//  }
//  
//}


