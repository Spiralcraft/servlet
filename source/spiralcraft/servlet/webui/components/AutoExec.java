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

import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.EventContext;

/**
 * <p>Automatically runs a command whenever the state frame changes.
 * </p>
 * 
 * <p>The command is either executed in the "request" or the "prepare"
 *   stage, depending on which stage encounters a state frame change, and
 *   whether the "whenX" expression returns "true" if supplied.
 * </p>
 * 
 * @author mike
 *
 */
public class AutoExec<Tcontext,Tresult>
  extends AbstractCommandControl<Tcontext,Tresult>
{
  

  private Binding<Boolean> whenX;
  private boolean delayUntilPrepare;
  
  
  public void setWhenX(Binding<Boolean> whenX)
  { this.whenX=whenX;
  }
  
  /**
   * Ensures that the command will only run at the "prepare" stage of
   *   the request. If the request is the first in a sequence, the command
   *   will run after all processing has been completed in the "request"
   *   stage.
   * 
   * @param delayUntilPrepare
   */
  public void setDelayUntilPrepare(boolean delayUntilPrepare)
  { this.delayUntilPrepare=delayUntilPrepare;
  }

  @Override
  public void handleRequest(ServiceContext context)
  {
    
    // Make sure we bypass the command if we're delaying it
    getState(context).setBypass(delayUntilPrepare);
    super.handleRequest(context);
  }
  
  
  @Override
  public void handlePrepare(ServiceContext context)
  {
    if (delayUntilPrepare)
    { 
      // If we bypassed the command on "request", make sure we don't bypass
      //   it now.
      getState(context).setBypass(false);
    }
    super.handlePrepare(context);
  }
  
  
  @Override
  protected void scatter(ServiceContext context)
  { tryToExec(context);    
  }

  
  @Override
  public String getVariableName()
  { return null;
  }
  
  private void tryToExec(ServiceContext context)
  {
    AutoExecState<Tcontext,Tresult> state=getState(context);
    if (state.getBypass())
    {
      // Run the command only once during a cycle
      return;
    }
  
    if (whenX==null || Boolean.TRUE.equals(whenX.get()))
    { 
      executeCommand(context);
      if (state.getValue()!=null)
      { state.setBypass(true);
      }
    }
    
  }
  
  
  @Override
  public Focus<?> bindSelf(Focus<?> focus)
    throws BindException
  { 
    
    focus=super.bindSelf(focus);
    if (whenX!=null)
    { whenX.bind(focus);
    }
    return focus;
  }  

  @Override
  protected void gather(ServiceContext context)
  { 
    // We're not doing anything here
   
    
  }  

  @Override
  public AutoExecState<Tcontext,Tresult> createState()
  { return new AutoExecState<Tcontext,Tresult>(this);
  }
  
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected AutoExecState<Tcontext,Tresult> getState(EventContext context)
  { return (AutoExecState) context.getState();
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

class AutoExecState<Tcontext,Tresult>
  extends CommandState<Tcontext,Tresult>
{
  private boolean bypass;
  
  public AutoExecState(AutoExec<Tcontext,Tresult> comp)
  { super(comp);
  }
  
  public void setBypass(boolean bypass)
  { this.bypass=bypass;
  }
  
  public boolean getBypass()
  { return bypass;
  }
  
}



