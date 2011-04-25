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
import spiralcraft.common.ContextualException;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.Setter;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.task.Scenario;
import spiralcraft.task.Task;


/**
 * Executes a command parameterized by the http request query arguments
 * 
 * @author mike
 *
 */
@SuppressWarnings("rawtypes")
public class TaskFilter<Tcontext,Tresult>
  extends FocusFilter<Command<Task,Tcontext,Tresult>>
{
  
  
  private ThreadLocalChannel<Command<Task,Tcontext,Tresult>> commandLocal;
  
  private Assignment<?>[] contextAssignments;
  private Assignment<?>[] commandAssignments;
  
  private Setter<?>[] commandSetters;
  private Setter<?>[] contextSetters;
  
  private ParameterBinding<?>[] queryBindings;
  
  private Scenario<Tcontext,Tresult> task;
  
  private boolean ignoreException=false;
  
  { setUsesRequest(true);
  }
  
  /**
   * The Task scenario to run
   * 
   * @param x
   */
  public void setTask(Scenario<Tcontext,Tresult> task)
  { this.task=task;
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
  @SuppressWarnings("unchecked")
  @Override
  protected Focus<Command<Task,Tcontext,Tresult>> createFocus
    (Focus<?> parentFocus)
    throws ContextualException
  { 

    task.bind(parentFocus);
    
    commandLocal
      =new ThreadLocalChannel<Command<Task,Tcontext,Tresult>>
        ( (Reflector<Command<Task,Tcontext,Tresult>>) task.getCommandReflector()
        ,true
        );
    
    Focus<Command<Task,Tcontext,Tresult>> focus
      =new SimpleFocus<Command<Task,Tcontext,Tresult>>(parentFocus,commandLocal);
    
    
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
    commandLocal.push(task.command());
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
