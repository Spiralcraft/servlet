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

import java.util.ArrayList;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.Field;
import spiralcraft.data.Type;

import spiralcraft.data.core.SequenceField;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;


/**
 * Provides lifecycle management and WebUI control bindings for
 *   BufferAggregates.
 * 
 * @author mike
 *
 */
public abstract class AggregateEditor<Tcontent extends DataComposite>
  extends EditorBase<BufferAggregate<BufferTuple,Tcontent>>
{
  protected static final ClassLog log
    =ClassLog.getInstance(AggregateEditor.class);

  protected ThreadLocalChannel<BufferTuple> childChannel;
  protected Focus<BufferTuple> childFocus;
  protected int padSize;
  protected Expression<Boolean> padX;
  protected Channel<Boolean> isPadChannel;
  
  private Setter<?>[] fixedSetters;
  private Setter<?>[] initialSetters;
  private Setter<?>[] defaultSetters;
  private Setter<?>[] newSetters;
  private Setter<?>[] preSaveSetters;
  
  private boolean contentRequired;
  private String contentRequiredMessage="At least one entry is required";
  
  public Command<BufferAggregate<BufferTuple,Tcontent>,Void,Void> 
    addNewCommand()
  {
    return new QueuedCommand<BufferAggregate<BufferTuple,Tcontent>,Void,Void>
      (getState()
      ,new CommandAdapter<BufferAggregate<BufferTuple,Tcontent>,Void,Void>()
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
   * <p>The number of incomplete unsaved Buffers that will be maintained.
   * </p>
   * 
   * <p>The "padX" property determines what is considered "padding".
   * </p>
   * @param padSize
   */
  public void setPadSize(int padSize)
  { this.padSize=padSize;
  }
  
  /**
   * <p>An Expression, evaluated against a child Buffer, which determines
   *   whether the buffer is "padding", or ignorable when saving.
   * </p>
   * 
   * <p>Defaults to an Expression which checks if any required field values
   *   are null
   * </p>
   * @param padX
   */
  public void setPadX(Expression<Boolean> padX)
  { this.padX=padX;
  }
  
  /**
   * Require at least one non-padding entry on save, or generate an error
   *   that will abort the save attempt.
   *   
   * @param contentRequired
   */
  public void setContentRequired(boolean contentRequired)
  { this.contentRequired=contentRequired;
  }
  
  /**
   * The error message that will be generated when contentRequired=true and
   *   the list does not contain at least one savable entry 
   * 
   * @param contentRequiredMessage
   */
  public void setContentRequiredMessage(String contentRequiredMessage)
  { this.contentRequiredMessage=contentRequiredMessage;
  }
  
  @Override
  protected void scatter(ServiceContext context)
  {
    super.scatter(context);
    BufferAggregate<BufferTuple,?> aggregate=getState().getValue();
    
    if (aggregate!=null)
    {
      if (debug)
      { log.fine("Scattering buffer "+aggregate);
      }

      for (BufferTuple buffer: aggregate)
      { initChild(buffer);
      }
      
      if (padSize>0)
      { makePadding(aggregate);
      }
      
    }

  }

  private void makePadding(BufferAggregate<BufferTuple,?> aggregate)
  { 
    int padCount=0;
    for (BufferTuple buffer: aggregate)
    {
    
      childChannel.push(buffer);
      try
      {
        if (Boolean.TRUE.equals(isPadChannel.get()))
        { padCount++;
        }
      }
      finally
      { childChannel.pop();
      }      
    }
    
    try
    {
      
      for (int i=padCount;i<padSize;i++)
      { aggregate.add(newChildBuffer());
      }
    }
    catch (DataException x)
    { 
      log.log
        (Level.SEVERE,toString()+": Error padding buffer for type "+getType()
        ,x);
    }
    
  }
  
  private void initChild(BufferTuple child)
  {
    if (newSetters==null && initialSetters==null)
    { return;
    }
    
    childChannel.push(child);
    try
    {

      if (newSetters!=null && child.getOriginal()==null)
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
    finally
    { childChannel.pop();
    }
  }
  
      
  @Override
  protected void save()
    throws DataException
  {
    BufferAggregate<BufferTuple,?> aggregate=getState().getValue();
    
    if (aggregate==null)
    {
      if (debug)
      { log.fine("BufferAggregate is null, not saving");
      }      
    }
    else
    {
      if (preSaveSetters!=null)
      {
        for (int i=0;i<aggregate.size();i++)
        { 
          BufferTuple buffer=aggregate.get(i);
          childChannel.push(buffer);
          try 
          { 
            if (debug)
            { log.fine(toString()+": applying preSaveAssignments to "+buffer);
            }
            Setter.applyArray(preSaveSetters);
          }
          finally
          { childChannel.pop();
          }
        }
      }

      beforeCheckDirty(aggregate);
      
      if (aggregate.isDirty())
      {

        int saveCount=0;
        for (int i=0;i<aggregate.size();i++)
        { 
          BufferTuple buffer=aggregate.get(i);
          if (buffer.isDirty())
          { 
            if (saveChild(buffer))
            { saveCount++;
            }
          }
        }
      
        if (contentRequired && saveCount==0)
        { throw new DataException(contentRequiredMessage);
        }
        aggregate.reset();
      }
      else
      { 
        if (debug)
        { log.fine("BufferAggregate is not dirty");
        }
      }
    }
    
  }  
  
  private boolean saveChild(BufferTuple child)
    throws DataException
  {
    childChannel.push(child);
    try
    {
      
      if (defaultSetters!=null)
      { 
        if (debug)
        { log.fine(toString()+": applying default values to "+child);
        }
        Setter.applyArrayIfNull(defaultSetters);      
      }

      if (fixedSetters!=null)
      {
        if (debug)
        { log.fine(toString()+": applying fixed values to "+child);
        }
        Setter.applyArray(fixedSetters);
      }
      
      if (isPadChannel==null 
          || !Boolean.TRUE.equals(isPadChannel.get())
          )
      { 
        child.save();
        return true;
      }
      else
      { return false;
      }
      
    }
    finally
    { childChannel.pop();
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
      throws BindException
  { 
    if (debug)
    { log.fine("Editor.bind() "+parentFocus);
    }
    Channel<?> source=super.bindTarget(parentFocus);
    
    if (source==null && type==null)
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
        if (debug)
        { log.fine("Using existing BufferChannel for "+source.getReflector());
        }
        bufferChannel=(Channel<Buffer>) source;
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
  @SuppressWarnings("unchecked")
  protected Focus<?> bindExports(Focus<?> focus)
    throws BindException
  {
    focus=super.bindExports(focus);
    DataReflector<Buffer> reflector
      =(DataReflector<Buffer>) focus.getSubject().getReflector();
  
    Type<?> contentType=reflector.getType().getContentType();

    if (!contentType.isAggregate() && contentType.getScheme()!=null)
    { 
      ArrayList<String> padFieldNames
        =new ArrayList<String>();
      
      for (Field<?> field: contentType.getFieldSet().fieldIterable())
      {
        
        if (field.isRequired() 
            && !(field instanceof SequenceField)
            && field.getDefaultExpression()==null
            )
        { padFieldNames.add(field.getName());
        }
        
        
      }
      
      childChannel=new ThreadLocalChannel<BufferTuple>
        (DataReflector.<BufferTuple>getInstance(contentType));

      childFocus=focus.chain(childChannel);
      
      fixedSetters=bindAssignments(childFocus,fixedAssignments);
      defaultSetters=bindAssignments(childFocus,defaultAssignments);
      newSetters=bindAssignments(childFocus,newAssignments);
      initialSetters=bindAssignments(childFocus,initialAssignments);
      preSaveSetters=bindAssignments(childFocus,preSaveAssignments);
      bindRequestAssignments(requestBindings);
      bindRequestAssignments(redirectBindings);
      
      if (padSize>0 && padX==null)
      { 
        if (padFieldNames.size()==0)
        { 
          throw new BindException
            ("No padX specified, and no candidate fields found");
        }
        StringBuffer expr=new StringBuffer();
        for (String name:padFieldNames)
        { 
          if (expr.length()>0)
          { expr.append(" || ");
          }
          expr.append(name+"==null ");
        }
        
        padX=Expression.<Boolean>create(expr.toString());
      }
      if (padX!=null)
      { isPadChannel=childFocus.bind(padX);
      }
      
    }
    
   
    
    
    
    return focus;
    
  }
  
  private void bindRequestAssignments(RequestBinding<?>[] bindings)
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
      binding.bind(childFocus);
    }
  }

  @SuppressWarnings("unchecked") // newBuffer() is not generic
  protected BufferAggregate<BufferTuple,Tcontent> newAggregate()
    throws DataException
  {
    // Create the aggregate buffer if none
    BufferAggregate<BufferTuple,Tcontent> aggregate
      =(BufferAggregate<BufferTuple,Tcontent>) 
        getDataSession().newBuffer(getType());
    writeToModel(aggregate);
    return aggregate;
  }
  
  /**
   * Add a new empty buffer
   */
  protected void addNewBuffer()
    throws DataException
  {
    BufferAggregate<BufferTuple,Tcontent> aggregate
      =getState().getValue();
    if (aggregate==null)
    { aggregate=newAggregate();
    }

    // Add a Tuple to the list
    aggregate.add(newChildBuffer());
      
  }
  
  protected BufferTuple newChildBuffer()
    throws DataException
  {
      BufferTuple newTuple
        =(BufferTuple) getDataSession().newBuffer(getType().getContentType());
      initChild(newTuple);
      return newTuple;
  }
}



