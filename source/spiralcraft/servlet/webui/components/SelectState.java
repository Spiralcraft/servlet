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

import java.util.ArrayList;
import java.util.List;

import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.util.ArrayUtil;

/**
 * Represents the state of a single or multiple selection
 * 
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
public class SelectState<Ttarget,Tvalue>
  extends ControlGroupState<Ttarget>
{
  
  public List<Tvalue> selected;
  
  public SelectState(AbstractSelectControl<Ttarget,Tvalue> control)
  { super(control);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void setValue(Ttarget targetVal)
  {
    super.setValue(targetVal);
    if (targetVal==null)
    { selected=null;
    }
    else
    {
      selected=new ArrayList<Tvalue>();
      // XXX Figure out cardinality beforehand
      if (targetVal instanceof Iterable)
      {
        for (Tvalue val : (Iterable<Tvalue>) targetVal)
        { selected.add(val);
        }
      }
      else if (targetVal.getClass().isArray())
      { 
        for (Tvalue val : ArrayUtil.<Tvalue>iterable((Tvalue[]) targetVal))
        { selected.add(val);
        }
      }
      else
      { selected.add((Tvalue) targetVal);
      }
    }
  }
  
  public boolean isSelected(Tvalue value)
  { 
    boolean ret;
    if (selected==null)
    { ret=false;
    }
    else
    { ret=selected.contains(value);
    }
    
    if (control.isDebug())
    { log.fine(control.toString()+": "+(ret?"SELECTED":"not selected")+" value="+value+" selected="+selected);
    }
    return ret;
  }
  
  
}