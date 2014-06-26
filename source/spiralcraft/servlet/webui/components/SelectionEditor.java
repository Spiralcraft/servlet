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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import spiralcraft.common.Coercion;
import spiralcraft.common.ContextualException;
import spiralcraft.data.Aggregate;
import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.lang.functions.Sort;
import spiralcraft.lang.kit.CoercionChannel;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.lang.NumericCoercion;

/**
 * <p>Manages the contents of an Aggregate that represents a
 *  one-to-many relationship, or a "relationized" list of
 *  primitives, which is modified by a selection provided
 *  by in incoming control (eg. a SelectList in multiselect mode)
 * </p>
 * 
 * @author mike
 *
 */
public class SelectionEditor
  <TorigContent extends DataComposite,TselectItem>
    extends AggregateEditor<TorigContent>
{
  
  private Expression<TselectItem> key;
  private Channel<TselectItem> keyChannel;
  private Expression<Integer> position;
  private Channel<Integer> positionChannel;
  private Channel<Aggregate<BufferTuple>> sortChannel;
  
  /**
   * Specify the selection expression from the perspective of
   *   the content
   *   
   * @param key
   */
  public void setSelectionKey(Expression<TselectItem> key)
  { this.key=key;
  }
  
  /**
   * <p>Specify the expression that references to location to store the
   *   relative position of each item in the list. The expression should
   *   begin with a "." if it is the name of a field in the target Type.
   * </p>
   *   
   * @param position
   */
  public void setPositionX(Expression<Integer> position)
  { this.position=position;
  }
  
  /**
   * <p>Provides the current selection
   * </p>
   * 
   */
  @SuppressWarnings("unchecked")
  protected TselectItem[] getSelectionKeys()
  {
    BufferAggregate<BufferTuple,?> aggregate=getState().getValue();
    
    if (aggregate==null)
    { 
      if (debug)
      { 
        log.fine
          ("getSelectionKeys() returning null because BufferAggregate is null");
      }
      return null;
    }
    ArrayList<TselectItem> items
      =new ArrayList<TselectItem>(aggregate.size());
    
    if (sortChannel==null)
    {
      for (BufferTuple buffer: aggregate)
      { 
        if (!buffer.isDelete())
        { items.add(evalKey(buffer));
        }
      }
    }
    else
    {
      for (BufferTuple buffer: sortChannel.get())
      {
        if (!buffer.isDelete())
        { items.add(evalKey(buffer));
        }
      }
    }
    
    TselectItem[] array
      =(TselectItem[]) Array.newInstance
        (keyChannel.getReflector().getContentType()
        ,items.size()
        );
    items.toArray(array);
    if (debug)
    { log.fine("getSelectionKeys() returning "+ArrayUtil.format(array,",","\""));
    }
    return array;
  }
  
  private TselectItem evalKey(BufferTuple buffer)
  { 
    childChannel.push(buffer);
    try
    { return keyChannel.get();
    }
    finally
    { childChannel.pop();
    }
    
  }
  
  
  /**
   * <p>Update the current selection, deleting and adding rows
   *   as appropriate
   * </p>
   * 
   */
  protected void setSelectionKeys(TselectItem[] keys)
    throws DataException
  {
    BufferAggregate<BufferTuple,?> aggregate=getState().getValue();
    

    if (keys==null || keys.length==0)
    {
      // Delete everything
      if (debug)
      { log.fine("setting selection to null");
      }
      if (aggregate!=null)
      {
        for (BufferTuple buffer : aggregate)
        { buffer.delete();
        }
      }
      return;
    }
    
    if (debug)
    { log.fine("setting selection to "+ArrayUtil.format(keys,",","\""));
    }
    
    
    HashMap<TselectItem,BufferTuple> keyMap
      =new HashMap<TselectItem,BufferTuple>();
    
    if (aggregate==null)
    { aggregate=newAggregate();
    }
    
    // Populate look-up map
    for (BufferTuple buffer: aggregate)
    { mapBuffer(keyMap,buffer);
    }
    
    // Check-off or create selected items
    for (int i=0;i<keys.length;i++)
    {
      TselectItem item=keys[i];
      
      BufferTuple buffer=keyMap.get(item);
      if (buffer==null)
      { 
        buffer=newChildBuffer();
        setKey(buffer,item);
        aggregate.add(buffer);
      }
      else
      {
        if (buffer.isDelete())
        { buffer.undelete();
        }
        keyMap.remove(item);
      }
      
      if (positionChannel!=null)
      { setPosition(buffer,i);
      }
    }
    
    // Delete un-selected items
    for (BufferTuple buffer : keyMap.values())
    { buffer.delete();
    }
    
  }

  /**
   * Associate all existing buffers with the unique input key value that
   *   marks it as selected.
   *   
   * @param map
   * @param buffer
   */
  private void mapBuffer
    (HashMap<TselectItem,BufferTuple> map
    ,BufferTuple buffer
    )
  { 
    childChannel.push(buffer);
    try
    { map.put(keyChannel.get(),buffer);
    }
    finally
    { childChannel.pop();
    }
    
  }
  
  /**
   * Set the position of the selection 
   * 
   * @param buffer
   * @param item
   */
  private void setPosition
    (BufferTuple buffer
    ,int position
    )
  { 
    childChannel.push(buffer);
    try
    { positionChannel.set(position);
    }
    finally
    { childChannel.pop();
    }
    
  }

  /**
   * Give a new buffer the appropriate selection key.
   * 
   * @param buffer
   * @param item
   */
  private void setKey
    (BufferTuple buffer
    ,TselectItem item
    )
  { 
    childChannel.push(buffer);
    try
    { keyChannel.set(item);
    }
    finally
    { childChannel.pop();
    }
    
  }
  
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  {
    focus=super.bindExports(focus);
    if (key==null)
    { 
      throw new BindException
        ("SelectionEditor.selectionKey: Expression must be specified");
    }
    keyChannel=childFocus.bind(key);

    
    Class keyClass=keyChannel.getContentType();
    Class arrayClass=Array.newInstance(keyClass, 0).getClass();
    
    if (position!=null)
    { 
      
      Channel positionSourceChannel=childFocus.bind(position);
      if (!Integer.class.isAssignableFrom(positionSourceChannel.getContentType()))
      { 
        if (String.class.isAssignableFrom(positionSourceChannel.getContentType()))
        { 
          positionChannel
            =LangUtil.ensureType(positionSourceChannel,Integer.class,focus);
        }
        else if (Number.class.isAssignableFrom(positionSourceChannel.getContentType()))
        { 
          positionChannel
            =new CoercionChannel<Number,Integer>
              (BeanReflector.<Integer>getInstance(Integer.class)
              ,positionSourceChannel
              ,(Coercion<Number,Integer>) NumericCoercion.instance(Integer.class)
              ,(Coercion) NumericCoercion.instance(positionSourceChannel.getContentType())
              );
        }
      }
      else
      { positionChannel=positionSourceChannel;
      }

      sortChannel=new Sort(position,false).bindChannel(bufferChannel,focus,null);
    }
    return focus.chain(new SelectionChannel(arrayClass));
  }
  
  class SelectionChannel
    extends AbstractChannel<TselectItem[]>
  {
    public SelectionChannel(Class<TselectItem[]> arrayClass)
    { super(BeanReflector.<TselectItem[]>getInstance(arrayClass));
    }
    
    @Override
    protected TselectItem[] retrieve()
    { return getSelectionKeys();
    }

    @Override
    protected boolean store(TselectItem[] val) 
    { 
      try
      { 
        setSelectionKeys(val);
        return true;
      }
      catch (DataException x)
      { throw new AccessException("Error storing selection "+val,x);
      }
      
    }
  }
  
}

