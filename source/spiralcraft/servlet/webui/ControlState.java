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

import spiralcraft.textgen.ElementState;
import spiralcraft.textgen.MementoState;

/**
 * An ElementState associated with a named Form control
 *
 */
public class ControlState<Tbuf>
  extends ElementState
{

  protected ControlGroupState<?> controlGroupState;
  protected final Control<?> control;
  private String variableName;
  private Tbuf value;
  private String error;
  

  public ControlState(Control<?> control)
  { 
    super(control.getChildCount());
    this.control=control;
  }
  
  @Override
  public void resolve()
  { 
    // controlGroupState=getParent().findElementState(ControlGroupState.class);
    
    int dist=control.getControlGroupStateDistance();
    if (dist>-1)
    { 
      System.err.println("Distance from "+getClass().getName()+"="+dist);
      for (int i=0;i<dist;i++)
      { System.err.println(getParent().getAncestor(i).getClass().getName());
      }
      controlGroupState=(ControlGroupState<?>) getParent().getAncestor(dist);
    }
    
    System.err.println("ControlState: ControlGroupState="+controlGroupState);
    
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
  
    
  /**
   * Update the value, resetting any error and indicating whether the value
   *   should be propogated.
   * 
   * @param value
   * @return true, if the value should be propogated, ie. when the value
   *   changes or when an error state is updated.
   */
  public boolean updateValue(Tbuf value)
  { 
    if (error!=null)
    { 
      this.value=value;
      error=null;
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
  
  public String getError()
  { return error;
  }
  
  public void setError(String error)
  { 
    this.error=error;
    if (error!=null && controlGroupState!=null)
    { controlGroupState.setErrorState(true);
    }
  }
}
