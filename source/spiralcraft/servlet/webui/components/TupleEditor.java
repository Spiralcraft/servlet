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



import spiralcraft.log.Level;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.Type;
import spiralcraft.data.lang.AggregateIndexTranslator;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.spi.TranslatorChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.EventContext;
import spiralcraft.util.ArrayUtil;




/**
 * Provides lifecycle management and WebUI control bindings for
 *   BufferTuples
 * 
 * @author mike
 *
 */
public abstract class TupleEditor
  extends EditorBase<BufferTuple>
{
  private static final ClassLog log
    =ClassLog.getInstance(TupleEditor.class);

  private Setter<?>[] fixedSetters;
  private Setter<?>[] initialSetters;
  private Setter<?>[] defaultSetters;
  private Setter<?>[] newSetters;
  private Setter<?>[] publishedSetters;

  private Channel<BufferAggregate<Buffer,?>> aggregateChannel;
  
  private boolean phantom;
  
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
  
  @Override
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
    
  /**
   * <p>Adds the referenced Buffer to the parent BufferAggregate and
   *   clears the Editor to accommodate a new Tuple
   * </p>
   * 
   * @return A new Command
   */
  public Command<BufferTuple,Void> addAndClearCommand()
  { 
    return new QueuedCommand<BufferTuple,Void>
      (getState()
      ,new CommandAdapter<BufferTuple,Void>()
        { 
          @Override
          public void run()
          { 
            addToParent();
            // XXX Should validate here
            getState().setValue(null);
          }
        }
      );
  }
  
  public Command<BufferTuple,Void> deleteCommand()
  { 
    return new QueuedCommand<BufferTuple,Void>
      (getState()
      ,new CommandAdapter<BufferTuple,Void>()
        {
          @Override
          public void run()
          { 
            try
            { 
              getState().getValue().delete();
            }
            catch (Exception x)
            { 
              getState().addError("Error deleting data:");
              getState().setException(x);
            }
          }
        }
      );
  }
  
  public Command<BufferTuple,Void> deleteCommand
    (final Command<?,?> chainedCommand)
  { 
    return new QueuedCommand<BufferTuple,Void>
      (getState()
      ,new CommandAdapter<BufferTuple,Void>()
        {
          @Override
          public void run()
          { 
            try
            { 
              getState().getValue().delete();
              if (chainedCommand!=null)
              { chainedCommand.execute();
              }
            }
            catch (Exception x)
            { 
              getState().addError("Error queuing command:");
              getState().setException(x);
            }
          }
        }
      );
  }

  @Override
  protected void scatter(ServiceContext context)
  {
    super.scatter(context);
    BufferTuple buffer=getState().getValue();
    
    if (buffer!=null)
    {
      if (debug)
      { log.fine("Scattering buffer "+buffer);
      }
      applyRequestBindings(context);
      setInitialValues(buffer);
    }

  }
  
    
  protected void setInitialValues(Buffer buffer)
  {
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
  protected void save()
    throws DataException
  {
    if (phantom)
    {
      if (debug)
      { log.fine("Editor with phantom=true skipping save. "+toString());
      }
    }
    
    Buffer buffer=getState().getValue();
    
    if (buffer!=null && buffer.isDirty())
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
      
      if (inspect((BufferTuple) buffer,getState()))
      {
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
   * Add a new empty buffer to the parent 
   */
  protected void addNewBuffer()
  {
    try
    {
      if (aggregateChannel!=null)
      {
        aggregateChannel.get()
          .add(getDataSession().newBuffer(getType().getContentType()));
      }
    }
    catch (DataException x)
    { 
      log.log(Level.WARNING,"Error adding new buffer",x);
      getState().setException(x);
    }
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
        if (dataReflector.getType().isAggregate())
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
        bufferChannel=new BufferChannel
          ((Focus<DataComposite>) parentFocus
          ,(Channel<DataComposite>) source
          );
      }
      
      setupType();
    }
    
    if (bufferChannel==null)
    { 
      throw new BindException
        ("Not a DataReflector "
          +parentFocus.getSubject().getReflector()
        );
          
    }
    
    setupSession(parentFocus);
    
    return bufferChannel;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  protected Focus<Buffer> bindExports()
    throws BindException
  {
    DataReflector<Buffer> reflector
    =(DataReflector<Buffer>) getFocus().getSubject().getReflector();
  
    Type<?> type=reflector.getType();

    if (!type.isAggregate() && type.getScheme()!=null)
    { 
      // TODO: Used to traverse fields here, now that's done by the updater
      //   restructure this
      
    }
    else
    { 
      if (type.isAggregate())
      { throw new BindException
          ("Cannot bind a TupleEditor to an aggregate type "+type);
      }
    }
    
    fixedSetters=bindAssignments(fixedAssignments);
    defaultSetters=bindAssignments(defaultAssignments);
    newSetters=bindAssignments(newAssignments);
    initialSetters=bindAssignments(initialAssignments);
    publishedSetters=bindAssignments(publishedAssignments);
    bindRequestAssignments(requestBindings);
    bindRequestAssignments(redirectBindings);
    
    
    return null;
    
  }
  
  /**
   * <p>Create a new Action target for the Form post
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  @Override
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
        newBuffer();
        applyRequestBindings(context);
        setInitialValues(getState().getValue());
        
      }
    };
  }   
  
  @SuppressWarnings("unchecked")
  private void bindRequestAssignments(RequestBinding[] bindings)
    throws BindException
  {
    if (bindings==null)
    { return;
    }

    for (RequestBinding binding:bindings)
    { 
      if (debug)
      { binding.setDebug(true);
      }
      binding.bind(getFocus());
    }
  }


}



