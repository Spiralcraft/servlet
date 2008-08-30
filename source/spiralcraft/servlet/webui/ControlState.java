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
package spiralcraft.servlet.webui;

import java.util.ArrayList;
import java.util.List;

import spiralcraft.command.Command;
import spiralcraft.log.ClassLogger;
import spiralcraft.textgen.ElementState;
import spiralcraft.textgen.MementoState;

/**
 * <p>An ElementState associated with a named Form control.
 * </p>
 * 
 * <p>The ControlState encapsulates a "value" of a particular type (Tbuf)
 *   which hold the internal state of of the Control at its render-point. 
 * </p> 
 *
 */
public class ControlState<Tbuf>
  extends ElementState
{
  public enum DataState
  {INIT
  ,GATHERED
  ,PROCESSED
  ,SCATTERED
  ,RENDERED
  };
  
  protected static final ClassLogger log=ClassLogger.getInstance(ControlState.class);

  
  protected ControlGroupState<?> controlGroupState;
  protected final Control<?> control;
  private String variableName;
  private Tbuf value;
  private List<String> errors;
  private Throwable exception;
  private ArrayList<Command<Tbuf,?>> commands;
  private DataState dataState=DataState.INIT;
  

  public ControlState(Control<?> control)
  { 
    super(control.getChildCount());
    this.control=control;
  }
  
  public ControlGroupState<?> getControlGroupState()
  { return controlGroupState;
  }
  
  public boolean isErrorState()
  { return errors!=null || exception!=null;
  }
  
  @Override
  public String toString()
  { 
    String stringValue=value!=null?value.toString():"(null)";
    if (stringValue.length()>256)
    { stringValue=stringValue.substring(0,256)+"...";
    }
    return super.toString()+"value="+stringValue;
  }
  
  @Override
  public void resolve()
  { 
    // controlGroupState=getParent().findElementState(ControlGroupState.class);
    
    int dist=control.getControlGroupStateDistance();
    if (dist>-1)
    { 
//      log.fine("Distance from "+getClass().getName()+"="+dist);
//      for (int i=0;i<dist;i++)
//      { log.fine(getParent().getAncestor(i).getClass().getName());
//      }
      controlGroupState=(ControlGroupState<?>) getParent().getAncestor(dist);
    }
    
//    log.fine("ControlState: ControlGroupState="+controlGroupState);
    
    // Determine local part of variable name
    String localName=control.getVariableName();
    if (localName==null && controlGroupState!=null)
    { localName=controlGroupState.nextLocalName();
    }
    
    if (controlGroupState!=null)
    { 
      // Determine contextual part of variable name
      variableName=controlGroupState.getVariableName();
      
      // Factor in any 'detail' iteration between the control and the control
      //   group
      int iterDist=control.getIterationStateDistance();
      if (iterDist>-1 && iterDist<dist)
      { 
        MementoState iterState
          =(MementoState) getParent().getAncestor(iterDist-1);
        if (variableName!=null)
        { 
          variableName
            =variableName.concat(".")
            .concat(Integer.toString(iterState.getIndex()));
        }
        else
        { variableName=Integer.toString(iterState.getIndex());
        }
      }
      
      if (variableName!=null)
      { variableName=variableName.concat(".").concat(localName);
      }
      else
      { variableName=localName;
      }
    }
    else
    { variableName=localName;
    }
  }

  public String getVariableName()
  { return variableName;
  }
  

  public void resetError()
  { 
    this.errors=null;
    this.exception=null;
  }
  
  /**
   * <p>Update the value, resetting any error and indicating whether the value
   *   should be propogated. If there is no change to the value (according to
   *   a comparison using .equals()), this method will return false.
   * </p>
   * 
   * 
   * @param value
   * @return true, if the value should be propogated, ie. when the value
   *   changes or when an error state is updated.
   */
  public boolean updateValue(Tbuf value)
  { 
    if (isErrorState())
    { 
      this.value=value;
      resetError();
      return true;
    }
    else
    { 
      if (this.value==null)
      {
        if (value!=null)
        { 
          this.value=value;
          return true;
        }
        else
        { return false;
        }
      }
      else if (value==null)
      { 
        this.value=null;
        return true;
      }
      else if (!this.value.equals(value))
      { 
        this.value=value;
        return true;
      }
      else
      { return false;
      }
    }    
  }
  
  public Tbuf getValue()
  { return value;
  }
  
  public void setValue(Tbuf value)
  { this.value=value;
  }
  
  public String[] getErrors()
  { 
    if (errors==null || errors.isEmpty())
    { return null;
    }
    else
    { return errors.toArray(new String[errors.size()]);
    }
  }
  
  public void addError(String error)
  { 
    if (errors==null)
    { errors=new ArrayList<String>();
    }
    errors.add(error);
    if (controlGroupState!=null)
    { controlGroupState.setErrorState(true);
    }
  }
  
  public Throwable getException()
  { return exception;
  }
  
  public void setException(Throwable exception)
  { 
    addError(exception.getMessage());
    this.exception=exception;
  }
  
  /**
   * Queue a Command for execution after the "gather" phase of
   *   reading the browser input.
   * 
   * @param command
   */
  public synchronized void queueCommand(Command<Tbuf,?> command)
  { 
    if (control.isDebug())
    { log.fine(control+"Queued command "+command);
    }
    if (commands==null)
    { commands=new ArrayList<Command<Tbuf,?>>(1);
    }
    if (command instanceof QueuedCommand)
    { command=((QueuedCommand<Tbuf,?>) command).getCommand();
    }
    commands.add(command);
  }
  
  public List<Command<Tbuf,?>> dequeueCommands()
  { 
    List<Command<Tbuf,?>> list=commands;
    commands=null;
    return list;
  }

  
  public DataState getDataState()
  { return dataState;
  }
    
  public void setDataState(DataState state)
  { 
    if (control.isDebug())
    { log.fine(control.toString()+" state="+state);
    }
    this.dataState=state;
  }
  
  public Control<?> getControl()
  { return control;
  }

}
