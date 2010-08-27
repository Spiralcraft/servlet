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
import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;

import spiralcraft.data.Type;
import spiralcraft.data.lang.AggregateIndexTranslator;
import spiralcraft.data.lang.DataReflector;

import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.DataSession;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;

import spiralcraft.lang.Setter;
import spiralcraft.lang.spi.TranslatorChannel;
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


public abstract class Editor
  extends ControlGroup<Buffer>
{
  private static final ClassLog log=ClassLog.getInstance(Editor.class);

  private static final SaveMessage SAVE_MESSAGE=new SaveMessage();
  
  private Channel<Buffer> bufferChannel;
  private Channel<BufferAggregate<Buffer,?>> aggregateChannel;
  
  private Type<?> type;
  private Channel<DataSession> sessionChannel;

  private String newActionName;
  
  private Assignment<?>[] fixedAssignments;
  private Assignment<?>[] initialAssignments;
  private Assignment<?>[] defaultAssignments;
  private Assignment<?>[] newAssignments;
  private Assignment<?>[] publishedAssignments;
  
  
  private Setter<?>[] fixedSetters;
  private Setter<?>[] initialSetters;
  private Setter<?>[] defaultSetters;
  private Setter<?>[] newSetters;
  private Setter<?>[] publishedSetters;

  private RequestBinding<?>[] requestBindings;
  
  private URI redirectOnSaveURI;
  
  private boolean autoCreate;
  private boolean detail;
  private boolean phantom;
  private boolean retain;
  
  {
    addHandler
      (new MessageHandler()
      {

        @Override
        public void handleMessage(EventContext context, Message message,
            boolean postOrder)
        { 
          if (!phantom && !postOrder && message.getType()==SaveMessage.TYPE)
          {
            try
            { save();
            }
            catch (DataException x)
            { handleException(context,x);
            }
          }
          
          if (!phantom && postOrder && message.getType()==SaveMessage.TYPE)
          {
            if (redirectOnSaveURI!=null
                && !Editor.this.getState().isErrorState()
                )
            { 
              if (debug)
              { log.fine("Redirecting to "+redirectOnSaveURI);
              }
              try
              { ((ServiceContext) context).redirect(redirectOnSaveURI);
              }
              catch (ServletException x)
              { handleException(context,x);
              }
            }
          }
        }
      });
  }
  
  public void setRedirectOnSaveURI(URI uri)
  { this.redirectOnSaveURI=uri;
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
   * The Editor will edit the content type of a parent BufferAggregate 
   * 
   * @param val
   */
  public void setDetail(boolean val)
  { detail=val;
  }
  
  
  /**
   * The contained buffer will not be saved by this editor. Usually used in
   *   conjunction with setDetail(true) to create an Editor that adds a new
   *   value to a parent BufferAggregate. 
   * 
   * @param val
   */
  public void setPhantom(boolean val)
  { phantom=val;
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
   * @param bindings
   */
  public void setRequestBindings(RequestBinding<?>[] bindings)
  { requestBindings=bindings;
  }
  
  public boolean isDirty()
  { 
    ControlGroupState<Buffer> state=getState();
    return state.getValue()!=null && state.getValue().isDirty();
  }
                     
  public Command<Buffer,Void,Void> revertCommand()
  { 
    return new QueuedCommand<Buffer,Void,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void,Void>()
        {
          @Override
          public void run()
          { getState().getValue().revert();
          }
        }
      );
  }

  public Command<Buffer,Void,Void> saveCommand()
  { 
    if (phantom)
    { throw new IllegalStateException("Cannot save a phantom Editor");
    }
    
    return new QueuedCommand<Buffer,Void,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void,Void>()
        { 
          @Override
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
   * @return A new Command
   */
  public Command<Buffer,Void,Void> saveAndClearCommand()
  { 
    if (phantom)
    { throw new IllegalStateException("Cannot save a phantom Editor");
    }

    return new QueuedCommand<Buffer,Void,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void,Void>()
        { 
          @Override
          public void run()
          { 
            getState().queueMessage(SAVE_MESSAGE);
            
            // Executes after the message is processed down the chain.
            getState().queueCommand
              (new CommandAdapter<Buffer,Void,Void>()
              {
                @Override
                public void run()
                { 
                  if (!getState().isErrorState())
                  { getState().updateValue(null);
                  }
                }
              }
              );
          }
        }
      );
  }

  /**
   * <p>Adds the referenced Buffer to the parent BufferAggregate and
   *   clears the Editor to accommodate a new Tuple
   * </p>
   * 
   * @return A new Command
   */
  public Command<Buffer,Void,Void> addAndClearCommand()
  { 
    return new QueuedCommand<Buffer,Void,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void,Void>()
        { 
          @Override
          public void run()
          { 
            addToParent();
            // XXX Should validate here
            getState().updateValue(null);
          }
        }
      );
  }

  public Command<Buffer,Void,Void> newCommand()
  {
    return new QueuedCommand<Buffer,Void,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void,Void>()
        { 
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

  public Command<Buffer,Void,Void> addNewCommand()
  {
    return new QueuedCommand<Buffer,Void,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void,Void>()
        { 
          @Override
          public void run()
          { 
            try
            { addNewBuffer();
            }
            catch (DataException x)
            { setException(x);
            }
            
          }
        }
      );
  }


  /**
   * <p>Adds a buffer to a parent AggregateBuffer
   * </p>
   * 
   */
  protected void addToParent()
  {
    if (aggregateChannel!=null)
    {
      BufferAggregate<Buffer,?> aggregate=aggregateChannel.get();
      Buffer buffer=getState().getValue();

      if (aggregate!=null && buffer!=null)
      { 
        if (debug)
        { log.fine("Adding buffer to parent "+aggregate+": buffer="+buffer);
        }
        aggregate.add(buffer);
      }
      else
      {
        if (aggregate==null)
        {
          if (debug)
          { log.fine("Not adding buffer to null parent: buffer="+buffer);
          }
        }
        else
        {
          if (debug)
          { log.fine("Not adding null buffer to parent "+aggregate);
          }
        }
      }
      
    }
  }
  
  /**
   * If an aggregate, add a new empty buffer, otherwise
   *   add a new empty buffer to the parent 
   */
  @SuppressWarnings("unchecked")
  protected void addNewBuffer()
    throws DataException
  {

    if (type.isAggregate())
    {
      if (getState().getValue()==null)
      { newBuffer();
      }

      // Add a Tuple to the list
      ((BufferAggregate<Buffer,?>) getState().getValue())
      .add(sessionChannel.get().newBuffer(type.getContentType()));
    }
    else if (aggregateChannel!=null)
    {
      aggregateChannel.get()
        .add(sessionChannel.get().newBuffer(type.getContentType()));
    }
  }
  
  /**
   * Create a new buffer
   */
  protected void newBuffer()
    throws DataException
  {
    Buffer buffer=sessionChannel.get().newBuffer(type);
    getState().updateValue
    ( buffer
    );
    bufferChannel.set(getState().getValue());
    if (buffer!=null && debug)
    { buffer.setDebug(true);
    }
    
  }
  
  
  protected void save()
    throws DataException
  {
    Buffer buffer=getState().getValue();
    
    if (buffer!=null && buffer.isTuple() && buffer.isDirty())
    {
      if (defaultSetters!=null)
      { 
        if (debug)
        { log.fine(toString()+": applying default values");
        }
        for (Setter<?> setter: defaultSetters)
        { 
          if (setter.getTarget().get()==null)
          { setter.set();
          }
        }
      
      }

      if (fixedSetters!=null)
      {
        if (debug)
        { log.fine(toString()+": applying fixed values");
        }
        for (Setter<?> setter: fixedSetters)
        { setter.set();
        }
      }
      
      
      buffer.save();
      
      if (publishedAssignments!=null)
      {
        if (debug)
        { log.fine(toString()+": applying published assignments post-save");
        }
        for (Setter<?> setter: publishedSetters)
        { setter.set();
        }
      }      
    }
    else
    { 
      if (buffer==null)
      {
        log.warning
          ("No buffer exists to save- no data read- try Editor.autoCreate");
      }
      else
      {
        if (debug)
        { log.fine("Not dirty "+buffer.toString());
        }
      }
    }
    
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

    super.handlePrepare(context);
    if (publishedSetters!=null)
    {
      if (debug)
      { log.fine(toString()+": applying published assignments on prepare");
      }
      for (Setter<?> setter: publishedSetters)
      { setter.set();
      }
    }
 
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

    
  private void applyRequestBindings(ServiceContext context)
  {
    if (requestBindings!=null)
    {
      for (RequestBinding<?> binding: requestBindings)
      { 
        if (debug)
        { log.fine("Applying requestBinding "+binding.getName());
        }
        binding.getBinding().read(context.getQuery());
        binding.publish(context);
      }
    }
  }
    
  @Override
  public void scatter(ServiceContext context)
  { 
    Buffer lastBuffer=getState().getValue();
   
    super.scatter(context);
    if (getState().getValue()==null)
    { 
      // Deal with new value being null
      if (lastBuffer==null)
      { 
        if (autoCreate)
        {
          // Current value was null, and newly scattered value was null
          try
          {
            newBuffer();
            if (debug)
            { log.fine("Created new buffer "+getState().getValue());
            }
          }
          catch (DataException x)
          { handleException(context,x);
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
          try
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
    
    Buffer buffer=getState().getValue();
    
    if (buffer!=null)
    {
      if (debug)
      { log.fine("Scattering buffer "+buffer);
      }
      applyRequestBindings(context);
      if (!buffer.isDirty())
      {
        if (newSetters!=null && buffer.getOriginal()==null)
        { 
          if (debug)
          { log.fine(toString()+": applying new values");
          }
          
          for (Setter<?> setter : newSetters)
          { setter.set();
          }
        }
        
        if (initialSetters!=null)
        {
          if (debug)
          { log.fine(toString()+": applying initial values");
          }

          for (Setter<?> setter : initialSetters)
          { setter.set();
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
      { responsive=false;
      }
      
      @Override
      public void invoke(ServiceContext context)
      { 
        if (debug)
        {
          log.fine
            ("Editor: Action invoked: "
            +ArrayUtil.format(getTargetPath(),"/",null)
            );
        }
        try
        {  newBuffer();
        }
        catch (DataException x)
        { handleException(context,x);
        }
        
        
      }
    };
  }  
  /**
   * Wraps default behavior and provides a BufferChannel that buffers what
   *   comes from the target expression.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Channel<Buffer> bindTarget
    (Focus<?> parentFocus)
      throws BindException
  { 
    if (debug)
    { log.fine("Editor.bind() "+parentFocus);
    }
    Channel<?> source=super.bindTarget(parentFocus);
    
    
    if (source==null)
    { 
      source=parentFocus.getSubject();
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
      DataReflector dataReflector=(DataReflector) source.getReflector();
      
      if ( dataReflector.getType() 
            instanceof BufferType
         ) 
      { 
        if (dataReflector.getType().isAggregate() && detail)
        {          
          bufferChannel
            =new TranslatorChannel
              (source
              ,new AggregateIndexTranslator(dataReflector)
              ,null // parentFocus.bind(indexExpression);
              );
          if (debug)
          { log.fine("Buffering indexed detail "+bufferChannel.getReflector());
          }
          aggregateChannel=(Channel<BufferAggregate<Buffer,?>>) source;
          
        }
        else
        {
          if (debug)
          { log.fine("Using existing BufferChannel for "+source.getReflector());
          }
          bufferChannel=(Channel<Buffer>) source;
        }
      }
      else
      {
        if (debug)
        { log.fine("Creating BufferChannel for "+source.getReflector());
        }
        bufferChannel=new BufferChannel<Buffer>
          ((Focus<DataComposite>) parentFocus
          ,(Channel<DataComposite>) source
          );
      }
      type=((DataReflector) bufferChannel.getReflector()).getType();
    }
    
    if (bufferChannel==null)
    { 
      throw new BindException
        ("Not a DataReflector "
          +parentFocus.getSubject().getReflector()
        );
          
    }
    
    sessionChannel=DataSession.findChannel(parentFocus);
    return bufferChannel;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  protected Focus<Buffer> bindExports(Focus<?> focus)
    throws BindException
  {
    DataReflector<Buffer> reflector
    =(DataReflector<Buffer>) focus.getSubject().getReflector();
  
    Type<?> type=reflector.getType();

    if (!type.isAggregate() && type.getScheme()!=null)
    { 
      // Used to go through fields here
      
    }
    
    fixedSetters=bindAssignments(focus,fixedAssignments);
    defaultSetters=bindAssignments(focus,defaultAssignments);
    newSetters=bindAssignments(focus,newAssignments);
    initialSetters=bindAssignments(focus,initialAssignments);
    publishedSetters=bindAssignments(focus,publishedAssignments);
    bindRequestAssignments(focus);
    
    
    return (Focus<Buffer>) focus;
    
  }
  

 
  @SuppressWarnings("unchecked")
  private void bindRequestAssignments(Focus<?> focus)
    throws BindException
  {
    if (requestBindings==null)
    { return;
    }

    for (RequestBinding binding:requestBindings)
    { 
      if (debug)
      { binding.setDebug(true);
      }
      binding.bind(focus);
    }
  }

}



