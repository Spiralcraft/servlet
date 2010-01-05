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


import spiralcraft.textgen.EventContext;


import spiralcraft.lang.BindException;
import spiralcraft.servlet.webui.components.AbstractSelectItemControl;
import spiralcraft.servlet.webui.components.SelectItemState;

/**
 * <P>Manages an OPTION as part of a SELECT list.
 * </P>
 * 
 * <P>Specify an expression for "value" property to define the "key" that
 *   gets sent back to the target of the SELECT list. This OPTION will
 *   render as selected when appropriate.
 * </P>
 * 
 * <P>The entry presented to
 *   the user is defined within the content of this Control. 
 *   Specify the optional "x" target property to provide a specific Focus
 *   in the chain for the display content of the option.
 * </P>
 * 
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
public class Option<Ttarget,Tvalue>
  extends AbstractSelectItemControl<Ttarget,Tvalue>
{

  
  private AbstractTag tag
    =new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "option";
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      SelectItemState<Tvalue> state=getState(context);

      if (state.isSelected())
      { renderAttribute(context.getWriter(),"selected","selected");
      }
      if (converter!=null)
      { 
        renderAttribute
          (context.getWriter(),"value",converter.toString(state.getValue())
          );
      }
      else
      {
        if (state.getValue()!=null)
        {
          renderAttribute
            (context.getWriter()
            ,"value"
            ,state.getValue()!=null
            ?state.getValue().toString()
            :""
            );
        }
      }
      super.renderAttributes(context);
      
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { Option.super.render(context);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
  };
  
  private ErrorTag errorTag=new ErrorTag(tag);
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    if (getState(context).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }

  @Override
  public void bindSelf()
    throws BindException
  { 
    tag.bind(getFocus());
    errorTag.bind(getFocus());
  }     
    
}


