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
import spiralcraft.data.session.Buffer;
import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.BufferChannel;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.data.session.BufferType;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.util.ArrayUtil;


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
  
  private Setter<?>[] fixedSetters;
  private Setter<?>[] initialSetters;
  private Setter<?>[] defaultSetters;
  private Setter<?>[] newSetters;
  
  public Command<BufferAggregate<BufferTuple,Tcontent>,Void> 
    addNewCommand()
  {
    return new QueuedCommand<BufferAggregate<BufferTuple,Tcontent>,Void>
      (getState()
      ,new CommandAdapter<BufferAggregate<BufferTuple,Tcontent>,Void>()
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
    else if (aggregate.isDirty())
    {

      for (int i=0;i<aggregate.size();i++)
      { 
        BufferTuple buffer=aggregate.get(i);
        if (buffer.isDirty())
        { saveChild(buffer);
        }
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
  
  private void saveChild(BufferTuple child)
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
        { log.fine(toString()+": applying fixed values to "+child);
        }
        for (Setter<?> setter: fixedSetters)
        { setter.set();
        }
      }
      
      
      child.save();
      
    }
    finally
    { childChannel.pop();
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
        bufferChannel=new BufferChannel
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
  protected Focus<?> bindExports()
    throws BindException
  {
    super.bindExports();
    DataReflector<Buffer> reflector
      =(DataReflector<Buffer>) getFocus().getSubject().getReflector();
  
    Type<?> contentType=reflector.getType().getContentType();

    if (!contentType.isAggregate() && contentType.getScheme()!=null)
    { 
      for (Field field: contentType.getFieldSet().fieldIterable())
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
      
      childChannel=new ThreadLocalChannel<BufferTuple>
        (DataReflector.<BufferTuple>getInstance(contentType));

      childFocus=new SimpleFocus(getFocus(),childChannel);
      
      fixedSetters=bindAssignments(fixedAssignments);
      defaultSetters=bindAssignments(defaultAssignments);
      newSetters=bindAssignments(newAssignments);
      initialSetters=bindAssignments(initialAssignments);
      bindRequestAssignments(requestBindings);
      bindRequestAssignments(redirectBindings);
      
    }
    
   
    
    
    
    return null;
    
  }
  
    
  @Override
  protected Setter<?>[] bindAssignments(Assignment<?>[] assignments)
    throws BindException
  {
    if (assignments!=null)
    {
      Setter<?>[] setters=new Setter<?>[assignments.length];
      int i=0;
      for (Assignment<?> assignment: assignments)
      { setters[i++]=assignment.bind(childFocus);
      }
      return setters;
    }
    return null;
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
    getState().setValue
      (aggregate
      );
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



