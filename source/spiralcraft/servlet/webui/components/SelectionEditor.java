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

import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.session.BufferAggregate;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.lang.spi.BeanReflector;
import spiralcraft.util.ArrayUtil;

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
    
    for (BufferTuple buffer: aggregate)
    { 
      if (!buffer.isDelete())
      { items.add(evalKey(buffer));
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
    { 
      if (!buffer.isDelete())
      { mapBuffer(keyMap,buffer);
      }
    }
    
    // Check-off or create selected items
    for (TselectItem item: keys)
    {
      BufferTuple buffer=keyMap.get(item);
      if (buffer==null)
      { 
        buffer=newChildBuffer();
        setKey(buffer,item);
        aggregate.add(buffer);
      }
      else
      { keyMap.remove(item);
      }
      
    }
    
    // Delete un-selected items
    for (BufferTuple buffer : keyMap.values())
    { buffer.delete();
    }
    
  }

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
  @SuppressWarnings("unchecked")
  protected Focus<?> bindExports()
    throws BindException
  {
    Focus<?> ret=super.bindExports();
    if (key==null)
    { 
      throw new BindException
        ("SelectionEditor.selectionKey: Expression must be specified");
    }
    keyChannel=childFocus.bind(key);
    if (ret!=null)
    { ret=getFocus();
    }
    
    Class keyClass=keyChannel.getContentType();
    Class arrayClass=Array.newInstance(keyClass, 0).getClass();
    
    SimpleFocus selectionFocus
      =new SimpleFocus
        (ret,new SelectionChannel(arrayClass));
    return selectionFocus;
  }
  
  class SelectionChannel
    extends AbstractChannel<TselectItem[]>
  {
    public SelectionChannel(Class<TselectItem[]> arrayClass)
      throws BindException
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

