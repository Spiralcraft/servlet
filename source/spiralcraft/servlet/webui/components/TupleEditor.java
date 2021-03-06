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




import spiralcraft.command.AbstractCommandFactory;
import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.command.CommandFactory;
import spiralcraft.common.ContextualException;
import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.Field;
import spiralcraft.data.Type;
import spiralcraft.data.lang.AggregateIndexTranslator;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.AccessException;
import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.spi.TranslatorChannel;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.app.Dispatcher;
import spiralcraft.util.ArrayUtil;

/**
 * <p>Provides lifecycle management and WebUI control bindings for
 *   BufferTuples
 * </p>
 * 
 * @author mike
 *
 */
public abstract class TupleEditor
  extends EditorBase<BufferTuple>
{
  private Setter<?>[] fixedSetters;
  private Setter<?>[] initialSetters;
  private Setter<?>[] defaultSetters;
  private Setter<?>[] newSetters;
  private Setter<?>[] publishedSetters;
  private Setter<?>[] preSaveSetters;
  private Binding<?> afterSave;
  private Binding<?> preSave;
  private Binding<?> onCreate;
  private Binding<?> onBuffer;
  private Binding<?> onAdd;

  private Channel<BufferAggregate<Buffer,?>> aggregateChannel;
  
  private boolean phantom;
  
  /**
   * Ensure that the current buffer will exist after saving
   */
  public final CommandFactory<BufferTuple,Void,Void> 
    ensureExists
      =new AbstractCommandFactory<BufferTuple,Void,Void>()
  {
    @Override
    public Command<BufferTuple,Void,Void> command()
    {
      return new QueuedCommand<BufferTuple,Void,Void>
        (getState()
        ,new CommandAdapter<BufferTuple,Void,Void>()
          { 
            { name="ensureExists";
            }

            @Override
            public void run()
            { 
              try
              { 
                BufferTuple tuple=getState().getValue();
                if (TupleEditor.this.debug)
                { log.fine("Existing value is "+tuple);
                }
                if (tuple==null)
                { 
                  newBuffer();
                  setInitialValues(getState().getValue());
                  applyRequestBindings(ServiceContext.get());
                  
                  if (TupleEditor.this.debug)
                  { log.fine("Created new buffer "+getState().getValue());
                  }
                }
                else if (tuple.isDelete())
                { tuple.undelete();
                }
              }
              catch (DataException x)
              { setException(x);
              }
            }
          }
        );
    }
  };
  
  
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
      { logFine("applying published assignments on prepare");
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
  public Command<BufferTuple,Void,Void> addAndClearCommand()
  { 
    return new QueuedCommand<BufferTuple,Void,Void>
      (getState()
      ,new CommandAdapter<BufferTuple,Void,Void>()
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
  
  /**
   * <p>Called by a control to queue the addAndClear action once the data
   *   is read.
   * </p>
   * 
   * @return A new Command
   */
  public void addAndClear()
  { addAndClearCommand().execute();
  }
  
  
  public Command<BufferTuple,Void,Void> deleteCommand()
  { return deleteCommand(null);
  }
  

  public void delete()
  { deleteCommand().execute();
  }
  
  public Command<BufferTuple,Void,Void> deleteCommand
    (final Command<?,?,?> chainedCommand)
  { 
    return new QueuedCommand<BufferTuple,Void,Void>
      (getState()
      ,new CommandAdapter<BufferTuple,Void,Void>()
        {
          @Override
          public void run()
          { 
            EditorState<BufferTuple> state=getState();
            if (state.getValue()!=null)
            {
              state.getValue().delete();
              if (chainedCommand!=null)
              { 
                chainedCommand.execute();
                if (chainedCommand.getException()!=null)
                { setException(chainedCommand.getException());
                }
              }
            }
            else
            {
              if (debug)
              { logFine("Nothing to delete");
              }
            }
          }
        }
      );
  }

  /**
   * <p>Indicate whether the Tuple being edited will exist if the Editor
   *   is saved. Returns true of a buffer is present and not deleted, or
   *   false if no buffer or a deleted buffer is present.
   * </p>
   * @return
   */
  public boolean getWillExist()
  { 
    BufferTuple buffer=getState().getValue();
    return buffer!=null && !buffer.isDelete();
  }
  
  @Override
  protected void scatter(ServiceContext context)
  {
    super.scatter(context);
    BufferTuple buffer=getState(context).getValue();
    
    if (buffer!=null)
    {
      if (debug)
      { 
        logFine("Scattering buffer "+buffer);
      }
      try
      {  
        setInitialValues(buffer);
        applyRequestBindings(context);
      }
      catch (AccessException x)
      { getState(context).setException(x);
      }
    
    }

  }
  
  
  protected void setInitialValues(Buffer buffer)
  {
    if (!buffer.isDirty())
    {
      if (buffer.getOriginal()==null)
      {
    
        if (newSetters!=null)
        {          
          if (debug)
          { logFine("applying newBindings");
          }
          for (Setter<?> setter : newSetters)
          { setter.set();
          }
        }
        
        if (onCreate!=null)
        { 
          if (debug)
          { logFine("applying onCreate");
          }
          onCreate.get();
        }
      }
        
      if (initialSetters!=null)
      {
        if (debug)
        { logFine("applying initialBindings");
        }

        for (Setter<?> setter : initialSetters)
        { setter.set();
        }
      }
      
      if (onBuffer!=null)
      { 
        
        if (debug)
        { logFine("applying onBuffer");
        }
        onBuffer.get();
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
        { logFine("applying requestBinding "+binding.getName());
        }
        binding.read(context.getQuery());
        binding.publish(context);
      }
    }
  }
      
  @Override
  protected void doSave()
    throws DataException
  {
    if (debug)
    { log.fine(getErrorContext()+" starting save of "+getState());
    }
    
    if (phantom)
    {
      if (debug)
      { logFine("Editor with phantom=true skipping save.");
      }
      return;
    }
    
    BufferTuple buffer=getState().getValue();
    if (buffer!=null)
    { 
      if (preSaveSetters!=null)
      { 
        if (debug)
        { logFine("applying preSaveAssignments");
        }
        Setter.applyArray(preSaveSetters);
      }
      
      if (preSave!=null)
      { preSave.get();
      }
      
      applyKeyValues();
      

      
      beforeCheckDirty(buffer);

      if (buffer.isDirty())
      {

        if (defaultSetters!=null)
        { 
          if (debug)
          { logFine("applying defaultAssignments");
          }
          Setter.applyArrayIfNull(defaultSetters);
        }
        
        if (fixedSetters!=null)
        {
          if (debug)
          { logFine("applying fixedAssignments");
          }
          Setter.applyArray(fixedSetters);
        }
      
        if (inspect(buffer,getState()))
        {
          if (debug)
          { logFine("Saving buffer "+buffer);
          }
          buffer.save();
          writeToModel(buffer);
          if (publishedAssignments!=null)
          {
            if (debug)
            { logFine("applying published assignments post-save");
            }
            for (Setter<?> setter: publishedSetters)
            { setter.set();
            }
          }
          
          if (afterSave!=null)
          { 
            Object result=afterSave.get();
            if (debug)
            { log.fine("afterSave returned "+result);
            }
          }
        }
        else
        {
          if (debug)
          { logFine("Inspection failed for "+buffer);
          }
        } 
        
      }
      else
      { 
        if (debug)
        { logFine("Not dirty "+buffer);
        }
      }
    }
    else
    {
      logWarning
        ("No buffer exists to save- no data read- try Editor.autoCreate");
    }
      
    
  }  
  

  /**
   * An expression to evaluate in the Tuple scope when a save is requested
   * 
   * @param preSave
   */
  public void setPreSave(Binding<?> preSave)
  { this.preSave=preSave;
  }
  
  /**
   * An expression to evaluate in the Tuple scope after the Tuple is saved.
   *   Use "afterSave" instead.
   * 
   */
  public void setOnSave(Binding<?> afterSave)
  { this.afterSave=afterSave;
  }

  /**
   * An expression to evaluate in the Tuple scope after the Tuple is saved
   * 
   */
  public void setAfterSave(Binding<?> afterSave)
  { this.afterSave=afterSave;
  }
  
  /**
   * An expression to evaluate in the Tuple scope when a new Tuple is created
   * 
   */
  public void setOnCreate(Binding<?> onCreate)
  { this.onCreate=onCreate;
  }

  /**
   * An expression to evaluate in the Tuple scope when a Tuple (new or existing)
   *   is buffered for editing
   * 
   * @param preSave
   */
  public void setOnBuffer(Binding<?> onBuffer)
  { this.onBuffer=onBuffer;
  }
  
  /**
   * An expression to evaluate in the Tuple scope right before a Tuple (new or existing)
   *   is added to a parent AggregateBuffer
   *   
   * @param onAdd
   */
  public void setOnAdd(Binding<?> onAdd)
  { this.onAdd=onAdd;
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
        if (onAdd!=null && buffer!=null)
        { onAdd.get();
        }
        
        if (debug)
        { logFine("Adding buffer to parent "+aggregate+": buffer="+buffer);
        }
        aggregate.add(buffer);
      }
      else
      {
        if (aggregate==null)
        {
          if (debug)
          { logFine("Not adding buffer to null parent: buffer="+buffer);
          }
        }
        else
        {
          if (debug)
          { logFine("Not adding null buffer to parent "+aggregate);
          }
        }
      }
      
    }
  }
    
  /**
   * Add a new empty buffer to the parent 
   */
  protected void addNewBuffer()
    throws DataException
  {
    if (aggregateChannel!=null)
    {
      Buffer buffer=getDataSession().newBuffer(getType().getContentType());
      if (debug)
      { buffer.setDebug(true);
      }
      aggregateChannel.get()
        .add(buffer);
    }
  }  

  /**
   * Wraps default behavior and provides a BufferChannel that buffers what
   *   comes from the target expression.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  protected Channel<Buffer> bindTarget
    (Focus<?> parentFocus)
      throws ContextualException
  { 
    if (debug)
    { logFine("Editor.bind() "+parentFocus);
    }
    Channel<?> source=super.bindTarget(parentFocus);
    

    
    if (source==null)
    { 
      source=parentFocus.getSubject();
      if (source==null)
      {
        logFine
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
          { logFine("Buffering indexed detail "+bufferChannel.getReflector());
          }
          aggregateChannel=(Channel<BufferAggregate<Buffer,?>>) source;
          
        }
        else
        {
          if (debug)
          { logFine("Using existing BufferChannel for "+source.getReflector());
          }
          bufferChannel=(Channel<Buffer>) source;
        }
      }
      else
      {
        if (debug)
        { logFine("Creating BufferChannel for "+source.getReflector());
        }
        bufferChannel=new BufferChannel<Buffer>
          ((Focus<DataComposite>) parentFocus
          ,(Channel<DataComposite>) source
          );
      }
      
      checkTypeCompatibility();
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
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected Focus<Buffer> bindExports(Focus<?> focus)
    throws ContextualException
  {
    focus=super.bindExports(focus);
    DataReflector<Buffer> reflector
    =(DataReflector<Buffer>) focus.getSubject().getReflector();
  
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
    
    for (Field<?> field:type.getFieldSet().fieldIterable())
    {
      if (field.getNewExpression()!=null)
      { 
        Assignment newAssignment
          =new Assignment
            (Expression.create(field.getName())
            ,field.getNewExpression()
            );
        if (newAssignments!=null)
        {
          newAssignments
            =ArrayUtil.append
              (newAssignments
              ,newAssignment
              );
        }
        else
        { newAssignments=new Assignment[] {newAssignment};
        }
      }
    }
    
    
    fixedSetters=bindAssignments(focus,fixedAssignments);
    defaultSetters=bindAssignments(focus,defaultAssignments);
    newSetters=bindAssignments(focus,newAssignments);
    initialSetters=bindAssignments(focus,initialAssignments);
    publishedSetters=bindAssignments(focus,publishedAssignments);
    preSaveSetters=bindAssignments(focus,preSaveAssignments);
    bindRequestAssignments(focus,requestBindings);
    bindRequestAssignments(focus,redirectBindings);
    if (afterSave!=null)
    { afterSave.bind(focus);
    }
    if (preSave!=null)
    { preSave.bind(focus);
    }
    if (onCreate!=null)
    { onCreate.bind(focus);
    }
    if (onBuffer!=null)
    { onBuffer.bind(focus);
    }
    if (onAdd!=null)
    { onAdd.bind(focus);
    }
    
    bindKeys(focus);
    return (Focus<Buffer>) focus;
    
  }
  
  /**
   * <p>Create a new Action target for the Form post
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  @Override
  protected Action createNewAction(Dispatcher context)
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
          logFine
            ("Editor: Action invoked: "+getName()+"@"
            +getTargetPath().format(".")
            );
        }
        try
        {
          newBuffer();
          applyRequestBindings(context);
          setInitialValues(getState(context).getValue());
        }
        catch (DataException x)
        { handleException(context,x);
        }
        
      }
    };
  }   
  
  private void bindRequestAssignments(Focus<?> focus,RequestBinding<?>[] bindings)
    throws BindException
  {
    if (bindings==null)
    { return;
    }

    for (RequestBinding<?> binding:bindings)
    { 
      if (debug)
      { binding.setDebug(true);
      }
      binding.bind(focus);
    }
  }


}



