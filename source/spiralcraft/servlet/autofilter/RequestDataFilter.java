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
package spiralcraft.servlet.autofilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.data.Tuple;
import spiralcraft.data.Type;
import spiralcraft.data.lang.TupleReflector;
import spiralcraft.data.spi.EditableArrayTuple;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.net.http.VariableMap;


/**
 * Provides access to data associated with a single request
 * 
 * @author mike
 *
 */
public class RequestDataFilter
  extends FocusFilter<Tuple>
{
  
  
  private ThreadLocalChannel<Tuple> dataChannel;
//  private String attributeName;
  private Type<Tuple> dataType;
  
  private Assignment<?>[] initialAssignments;
  
  private Setter<?>[] newSetters;
  private Setter<?>[] initialSetters;
  
  private ParameterBinding<?>[] queryBindings;
  
  
  public void setDataType(Type<Tuple> dataType)
  { this.dataType=dataType;
  }
  
  
  /**
   * Specify assignments that will be performed after any queryBindings
   *   are read.
   * 
   * @param assignments
   */
  public void setAssignments(Assignment<?>[] assignments)
  { this.initialAssignments=assignments;
  }
  
  public void setQueryBindings(ParameterBinding<?>[] queryBindings)
  { this.queryBindings=queryBindings;
  }
  
  /**
   * Called -once- to create the Focus
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Focus<Tuple> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    
//    this.attributeName=this.getPath().format("/")+"!"+
//      (dataType!=null?dataType.getURI():"");
    
    TupleReflector<Tuple> reflector
      =(TupleReflector) TupleReflector.getInstance(dataType);
    
    dataChannel
      =new ThreadLocalChannel<Tuple>(reflector);
    
    Focus<Tuple> focus
      =new SimpleFocus<Tuple>(parentFocus,dataChannel);
    
    Assignment<?>[] newAssignments = reflector.getNewAssignments();
    if (newAssignments!=null)
    { newSetters = Assignment.bindArray(newAssignments,focus);  
    }
    
    if (initialAssignments!=null)
    { initialSetters=Assignment.bindArray(initialAssignments,focus);
    }
    
    if (queryBindings!=null)
    {
      for (ParameterBinding<?> binding: queryBindings)
      { 
        binding.setRead(true);
        binding.bind(focus);
      }
    }
    
    return focus;
    
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  {
    dataChannel.pop();
  }
  
  

  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
  {
    dataChannel.push(new EditableArrayTuple(dataType));
    Setter.applyArray(newSetters);
    if (queryBindings!=null && request.getQueryString()!=null)
    { 
      VariableMap map
        =VariableMap.fromUrlEncodedString(request.getQueryString());
      for (ParameterBinding<?> binding: queryBindings)
      { binding.read(map);
      }
    }
    Setter.applyArray(initialSetters);
    
  }

}
