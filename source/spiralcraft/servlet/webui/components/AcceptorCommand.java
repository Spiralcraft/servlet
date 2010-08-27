//
//Copyright (c) 1998,2008 Michael Toth
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

import java.io.IOException;
import java.util.List;


import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.EventContext;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.net.http.VariableMap;

/**
 * <p>Triggers Command execution during the GATHER phase of an Acceptor,
 *   which occurs when the Acceptor is actioned. 
 * </p>
 * 
 * <p>Can be triggered either by the value of the "name" property appearing
 *   in a "command" variable in the POST or URI query
 *   String, and/or when a boolean condition specified in the "whenX" property
 *   evaluates to "true".
 * </p>
 * 
 * <p>If neither "name" or "whenX" are specified, the Command will be triggered
 *   every time the form receives a POST.
 * </p>
 *
 * <p>If both "name" and "whenX" are specified, the Command will be triggered
 *   only when both conditions are satisfied.
 * </p>
 *  
 * 
 * @author mike
 *
 */
public class AcceptorCommand<Tcontext,Tresult>
  extends AbstractCommandControl<Tcontext,Tresult>
{

  private String name;
  private Expression<Boolean> when;
  private Channel<Boolean> whenChannel;
    
  /**
   * <p>The name as it should appear in the values of the "command" post/query
   *   string to trigger this command.
   * </p>
   * @param name
   */
  public void setName(String name)
  { this.name=name;
  }

  /**
   * An boolean Expression that must be satisfied for the Command to
   *   be executed.
   * 
   * @param whenX
   */
  public void setWhenX(Expression<Boolean> whenX)
  { this.when=whenX;
  }
  
  @Override
  public String getVariableName()
  { return name;
  }
  
  
  @Override
  protected Focus<?> bindSelf(Focus<?> focus)
    throws BindException
  {
    focus=super.bindSelf(focus);
    if (when!=null)
    { whenChannel=focus.bind(when);
    }
    return focus;
  }

  
  @Override
  public void gather(ServiceContext context)
  {
    boolean triggered=false;
    
    if (name!=null)
    {
      VariableMap query=context.getQuery();
      if (query!=null)
      { 
        List<String> result=query.get("command");
        if (result!=null && result.contains(name))
        { triggered=true;
        }
      }
      
      VariableMap post=context.getPost();
      if (!triggered && post!=null)
      {
        List<String> result=post.get("command");
        if (result!=null && result.contains(name))
        { triggered=true;
        }
      }
      
      if (triggered && whenChannel!=null)
      { 
        Boolean val=whenChannel.get();
        triggered=val!=null && val;
      }
    }
    else if (whenChannel!=null)
    {   
      Boolean val=whenChannel.get();
      triggered=val!=null && val;
    }
    else
    { triggered=true;
    }
    
    if (triggered)
    { 
      
      if (debug)
      { 
        log.fine
          ("Command triggered"
          );
      }
      
      executeCommand(context);
      
    }
    else
    {
      if (debug)
      { log.fine("Not triggered");
      }
    }
    
    
  }
  
  @Override
  public void scatter(ServiceContext context)
  { 
  }

  @Override
  public void render(EventContext context)
    throws IOException
  { 
    pushState(context);
    try
    { super.render(context);
    }
    finally
    { popState(context);
    }
  }  

}

