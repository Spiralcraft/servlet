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

import spiralcraft.app.State;
import spiralcraft.command.Command;

import spiralcraft.log.ClassLog;
import spiralcraft.rules.RuleException;
import spiralcraft.rules.Violation;
import spiralcraft.textgen.MementoState;
import spiralcraft.util.Sequence;

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
  extends ComponentState
{  
  protected static final ClassLog log=ClassLog.getInstance(ControlState.class);

  
  protected ControlGroupState<?> controlGroupState;
  protected final Control<?> control;
  private String variableName;
  private Tbuf value;
  private Tbuf previousValue;
  private List<String> errors;
  private Throwable exception;
  private ArrayList<Command<Tbuf,?,?>> commands;
//  private DataState dataState=DataState.INIT;
  private boolean presented;
  

  public ControlState(Control<?> control)
  { 
    super(control);
    this.control=control;
  }
   
  public void setPresented(boolean presented)
  { this.presented=presented;
  }
  
  public boolean getPresented()
  { return presented;
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
    return super.toString()+" value="+stringValue;
  }
  
  @Override
  public void link(State parentState,Sequence<Integer> path)
  { 
    super.link(parentState,path);
    // controlGroupState=getParent().findElementState(ControlGroupState.class);
    
    int parentGroupDist=control.getControlGroupStateDistance();
    if (parentGroupDist>-1)
    { 
//      log.fine("Distance from "+getClass().getName()+"="+dist);
//      for (int i=0;i<dist;i++)
//      { log.fine(getParent().getAncestor(i).getClass().getName());
//      }
      controlGroupState=(ControlGroupState<?>) getParent().getAncestor(parentGroupDist);
    }
    
//    log.fine("ControlState: ControlGroupState="+controlGroupState);
    
    // Determine local part of variable name
    String localName=control.getVariableName();
    if (localName==null && controlGroupState!=null)
    { localName=controlGroupState.nextLocalName();
    }
    
    if (controlGroupState!=null && control.getContextualizeName())
    { 
      // Determine contextual part of variable name
      variableName=controlGroupState.getVariableName();
//       log.fine(getControl().toString()+" got variable prefix ["+variableName+"] + ["+localName+"] from "+controlGroupState.getControl());
      
      // Factor in any 'detail' iteration between the control and the control
      //   group
      int iterDist=control.getIterationStateDistance();
      if (iterDist>-1 && iterDist<parentGroupDist)
      { 
        MementoState iterState
          =(MementoState) getParent().getAncestor(iterDist-1);
        if (variableName!=null)
        { 
          variableName
            =variableName.concat(".")
            .concat(Integer.toString(iterState.getIndex()))
            .concat("-")
            .concat(localName);
        }
        else
        { 
          variableName=Integer.toString(iterState.getIndex())
            .concat("-")
            .concat(localName);
        }
      }
      else if (variableName!=null)
      { variableName=variableName.concat(".").concat(localName);
      }
      else
      { variableName=localName;
      }
    }
    else
    { 
      
      variableName=localName;
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
   * <p>Update the value and the record of the previous value. Calls
   *   setValue(value) to allow subtype to update dependent data.
   * </p>
   * 
   * 
   * @param value
   */
  public final void updateValue(Tbuf value)
  { 
    setValue(value);
    this.previousValue=value;
  }
  
  /**
   * Indicate that the model value is up-to-date by copying the current value
   *   to the record of the previousValue.
   */
  public void valueUpdated()
  { this.previousValue=value;
  }
  
  public Tbuf getValue()
  { return value;
  }
  
  public Tbuf getPreviousValue()
  { return previousValue;
  }
  
  /**
   * <p>Update the value, without updating the record of the previous value
   * </p>
   * 
   * <p>Use when storing an intermediate value that is not synchronized
   *   with the model. Called by updateValue() when it synchronizes the 
   *   internal value with the model.
   * </p>
   * 
   * <p>Override to ensure that any dependent data gets updated
   * </p>
   * @param value
   */
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
    if (exception instanceof RuleException)
    {
      RuleException re=(RuleException) exception;
      for (Violation<?> v : re.getViolations())
      { addError(v.getMessage());
      }
    }
    else
    { 
      if (exception.getMessage()!=null)
      { 
        if (exception.getCause()==null 
            || !exception.getMessage().equals(exception.getCause().toString())
            )
        { 
          // Keep programmatic strings from coming up into the UI
          addError(exception.getMessage());
        }
      }
      else if (exception.getCause()==null 
              && (this.errors==null || this.errors.isEmpty())
              )
      { 
        // Only do this if we have nothing else to display and nothing
        //   was added to the errors list.
        addError(exception.toString());
      }
    }
    if (exception.getCause()!=null)
    { 
      // Recursively go through causes and add error information
      setException(exception.getCause());
    }
    this.exception=exception;
  }
  
  /**
   * Queue a Command for execution after the "gather" phase of
   *   reading the browser input.
   * 
   * @param command
   */
  public synchronized void queueCommand(Command<Tbuf,?,?> command)
  { 
    if (control.isDebug())
    { log.fine(control+": Queued command "+command);
    }
    if (commands==null)
    { commands=new ArrayList<Command<Tbuf,?,?>>(1);
    }
    if (command instanceof QueuedCommand<?,?,?>)
    { command=((QueuedCommand<Tbuf,?,?>) command).getCommand();
    }
    commands.add(command);
  }
  
  public List<Command<Tbuf,?,?>> dequeueCommands()
  { 
    List<Command<Tbuf,?,?>> list=commands;
    commands=null;
    return list;
  }
  
  public Control<?> getControl()
  { return control;
  }

}
