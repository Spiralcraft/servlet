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
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.servlet.autofilter.spi.FocusFilter;


/**
 * Provides access to data shared across an application (servlet context)
 * 
 * @author mike
 *
 */
public class SharedDataFilter
  extends FocusFilter<Tuple>
{
  
  
  private Assignment<?>[] initialAssignments;
  private SimpleChannel<Tuple> dataChannel;
//  private String attributeName;
  private Type<Tuple> dataType;

  public void setAssignments(Assignment<?>[] assignments)
  { initialAssignments=assignments;
  }  
  
  public void setDataType(Type<Tuple> dataType)
  { this.dataType=dataType;
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
      =new SimpleChannel<Tuple>
        (reflector,new EditableArrayTuple(dataType),true);
    
    Focus<Tuple> focus
      =new SimpleFocus<Tuple>(parentFocus,dataChannel);
    
    
    Assignment<?>[] newAssignments = reflector.getNewAssignments();
    if (newAssignments!=null)
    {
      Setter<?>[] newSetters = Assignment.bindArray(newAssignments,focus);  
      Setter.applyArray(newSetters);
    }

    if (initialAssignments!=null)
    { 
      Setter<?>[] initialSetters
        =Assignment.bindArray(initialAssignments,focus);
      Setter.applyArray(initialSetters);
    }

    return focus;
    
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  {
    
  }
  
  

  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
    throws BindException
  {

  }

}
