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
import spiralcraft.data.DataException;
import spiralcraft.data.Field;
import spiralcraft.data.Type;
import spiralcraft.data.lang.DataReflector;

import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.DataSession;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.log.ClassLogger;

import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.UIMessage;
import spiralcraft.servlet.webui.VariableMapBinding;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;
import spiralcraft.textgen.MessageHandler;
import spiralcraft.util.ArrayUtil;


public abstract class Editor
  extends ControlGroup<Buffer>
{
  private static final ClassLogger log=ClassLogger.getInstance(Editor.class);

  private static final SaveMessage SAVE_MESSAGE=new SaveMessage();
  
  private Channel<Buffer> bufferChannel;
  private Type<?> type;
  private Channel<DataSession> sessionChannel;

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
  
  private boolean autoCreate;
  
  {
    addHandler
      (new MessageHandler()
      {

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
            { Editor.this.getState().setException(x);
            }
          }
        }
      });
  }
  
  public void setAutoCreate(boolean val)
  { autoCreate=val;
    
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
  public void setRequestBindings(RequestBinding[] bindings)
  { requestBindings=bindings;
  }
  
  
                     
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
          { getState().queueMessage(SAVE_MESSAGE);
          }
        }
      );
  }
  
  public Command<Buffer,Void> newCommand()
  {
    return new QueuedCommand<Buffer,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void>()
        { 
          public void run()
          { newBuffer();
          }
        }
      );
  }

  public Command<Buffer,Void> addNewCommand()
  {
    return new QueuedCommand<Buffer,Void>
      (getState()
      ,new CommandAdapter<Buffer,Void>()
        { 
          public void run()
          { addNewBuffer();
          }
        }
      );
  }

  protected void addNewBuffer()
  {
    try
    {
      if (type.isAggregate())
      {
        if (getState().getValue()==null)
        {
          // Create the aggregate buffer if none
          getState().setValue
          (sessionChannel.get().newBuffer(type)
          );
          bufferChannel.set(getState().getValue());

        }

        // Add a Tuple to the list
        ((BufferAggregate<?>) getState().getValue())
        .add(sessionChannel.get().newBuffer(type.getContentType()));
      }
    }
    catch (DataException x)
    { getState().setException(x);
    }
  }
  
  protected void newBuffer()
  {
    try
    {
      getState().setValue
      (sessionChannel.get().newBuffer(type)
      );
      bufferChannel.set(getState().getValue());
    }
    catch (DataException x)
    { 
      x.printStackTrace();
      getState().setException(x);
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
      log.warning
        ("No buffer exists to save- no data read- try Editor.autoCreate");
    }
    
  }
  
  protected void handlePrepare(ServiceContext context)
  { 
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
        if (autoCreate)
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
          { log.fine("Replacing last buffer with null (autoCreate==false)");
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
   * Wraps default behavior and provides a BufferChannel that buffers what
   *   comes from the target expression.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Channel<Buffer> extend
    (Focus<?> parentFocus)
      throws BindException
  { 
    log.fine("Editor.bind() "+parentFocus);
    Channel<?> source=(Channel<DataComposite>) 
      super.extend(parentFocus);
    
    
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
      DataReflector dataReflector=(DataReflector) source.getReflector();
      
      if ( dataReflector.getType() 
            instanceof BufferType
         ) 
      { 
        log.fine("Using existing BufferChannel for "+source.getReflector());
        bufferChannel=(Channel<Buffer>) source;
      }
      else
      {
        log.fine("Creating BufferChannel for "+source.getReflector());
        bufferChannel=new BufferChannel
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
    
    Focus sessionFocus=parentFocus.findFocus(DataSession.FOCUS_URI);
    if (sessionFocus!=null)
    { sessionChannel=sessionFocus.getSubject();
    }
    
    return bufferChannel;
  }
  
  @SuppressWarnings("unchecked")
  protected Focus<Buffer> bindSelf()
    throws BindException
  {
    DataReflector<Buffer> reflector
    =(DataReflector<Buffer>) getFocus().getSubject().getReflector();
  
    Type<?> type=reflector.getType();

    if (!type.isAggregate() && type.getScheme()!=null)
    { 
      for (Field field: type.getScheme().fieldIterable())
      {
        Expression<?> expression=field.getDefaultExpression();
        if (expression!=null)
        { 
          Assignment<?> assignment
            =new Assignment(Expression.create(field.getName()),expression);
          defaultAssignments
            =(Assignment[]) ArrayUtil.append(defaultAssignments,assignment);
        }
      }
      
    }
    
    fixedSetters=bindAssignments(fixedAssignments);
    defaultSetters=bindAssignments(defaultAssignments);
    newSetters=bindAssignments(newAssignments);
    initialSetters=bindAssignments(initialAssignments);
    publishedSetters=bindAssignments(publishedAssignments);
    bindRequestAssignments();
    
    
    return null;
    
  }
  
  private Setter<?>[] bindAssignments(Assignment<?>[] assignments)
    throws BindException
  {
    if (assignments!=null)
    {
      Setter<?>[] setters=new Setter<?>[assignments.length];
      int i=0;
      for (Assignment<?> assignment: assignments)
      { setters[i++]=assignment.bind(getFocus());
      }
      return setters;
    }
    return null;
  }
 
  @SuppressWarnings("unchecked")
  private void bindRequestAssignments()
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
      binding.bind(getFocus());
    }
  }

}

class SaveMessage
  extends UIMessage
{
  public static final MessageType TYPE=new MessageType();

  public SaveMessage()
  { 
    super(TYPE);
    transactional=true;
    multicast=true;
  }
}

