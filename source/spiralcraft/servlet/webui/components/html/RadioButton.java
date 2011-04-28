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
package spiralcraft.servlet.webui.components.html;

import java.io.IOException;


import spiralcraft.app.Dispatcher;


import spiralcraft.servlet.webui.components.AbstractSelectItemControl;
import spiralcraft.servlet.webui.components.SelectItemState;

/**
 * <p>Manages an OPTION as part of a SELECT list.
 * </p>
 * 
 * <p>Specify an expression for "value" property to define the "key" that
 *   gets sent back to the target of the SELECT list. This OPTION will
 *   render as selected when appropriate.
 * </p>
 * 
 * <p>The entry presented to
 *   the user is defined within the content of this Control. 
 *   Specify the optional "x" target property to provide a specific Focus
 *   in the chain for the display content of the option.
 * </p>
 * 
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
public class RadioButton<Ttarget,Tvalue>
  extends AbstractSelectItemControl<Ttarget,Tvalue>
{

  
  class Tag 
    extends AbstractTag
  {
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "input";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      SelectItemState<Tvalue> state=getState(context);


      renderAttribute(out,"type","radio");
      renderAttribute
        (out,"name",state.getSelectState().getVariableName());
      if (state.isSelected())
      { renderAttribute(out,"checked","checked");
      }
      if (converter!=null)
      { 
        renderAttribute
          (out,"value",converter.toString(state.getValue())
          );
      }
      else
      {
        if (state.getValue()!=null)
        {
          renderAttribute
            (out
            ,"value"
            ,state.getValue()!=null
            ?state.getValue().toString()
            :""
            );
        }
      }
      super.renderAttributes(context,out);
      
    }

    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
  };
  
  private Tag tag=new Tag();
  private ErrorTag errorTag=new ErrorTag();
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
    
  
  { 
    addHandler(errorTag);
    addHandler(tag);
  }
  
}


