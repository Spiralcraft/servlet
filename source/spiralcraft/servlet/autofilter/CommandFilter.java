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

import spiralcraft.command.Command;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.autofilter.spi.FocusFilter;


/**
 * Executes a command parameterized by the http request query arguments
 * 
 * @author mike
 *
 */
@SuppressWarnings("rawtypes")
public class CommandFilter
  extends FocusFilter<Command>
{
  
  
  private ThreadLocalChannel<Command> commandLocal;
  
  private Assignment<?>[] contextAssignments;
  private Assignment<?>[] commandAssignments;
  
  private Setter<?>[] commandSetters;
  private Setter<?>[] contextSetters;
  
  private ParameterBinding<?>[] queryBindings;
  
  private Binding<Command> x;
  
  private boolean ignoreException=false;
  
  { setUsesRequest(true);
  }
  
  /**
   * An Expression which returns a new instance of a Command to execute
   * 
   * @param x
   */
  public void setX(Binding<Command> x)
  { this.x=x;
  }
  
  /**
   * Do not write the exception to the log or return an error response
   *   in the event of a command exception
   */
  public void setIgnoreException(boolean ignoreException)
  { this.ignoreException=ignoreException;
  }
  
  
  /**
   * Specify assignments that will be performed after any queryBindings
   *   are read.
   * 
   * @param assignments
   */
  public void setContextAssignments(Assignment<?>[] assignments)
  { this.contextAssignments=assignments;
  }
  
  public void setQueryBindings(ParameterBinding<?>[] queryBindings)
  { this.queryBindings=queryBindings;
  }
  
  /**
   * Called -once- to create the Focus
   */
  @Override
  protected Focus<Command> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 

    x.bind(parentFocus);
    
    commandLocal
      =new ThreadLocalChannel<Command>(x,true);
    
    Focus<Command> focus
      =new SimpleFocus<Command>(parentFocus,commandLocal);
    
    
    if (commandAssignments!=null)
    { commandSetters = Assignment.bindArray(commandAssignments,focus);  
    }
    
    Focus contextFocus=focus.chain(commandLocal.resolve(focus,"context",null));
    if (contextAssignments!=null)
    { contextSetters=Assignment.bindArray(contextAssignments,contextFocus);
    }
    
    if (queryBindings!=null)
    {
      for (ParameterBinding<?> binding: queryBindings)
      { 
        binding.setRead(true);
        binding.bind(contextFocus);
      }
    }
    

    return focus;
    
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  {
    commandLocal.pop();
  }
  

  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
  {
    commandLocal.push();
    try
    {
      Setter.applyArray(commandSetters);
      if (queryBindings!=null && request.getQueryString()!=null)
      { 
        VariableMap map
          =VariableMap.fromUrlEncodedString(request.getQueryString());
        for (ParameterBinding<?> binding: queryBindings)
        { binding.read(map);
        }
      }
      Setter.applyArray(contextSetters);
      commandLocal.get().execute();
      
      if (commandLocal.get().getException()!=null && !ignoreException)
      {
        throw new RuntimeException
          ("Error executing command",commandLocal.get().getException());
      }
    }
    catch (RuntimeException x)
    { 
      commandLocal.pop();
      throw x;
    }
    
  }

}
