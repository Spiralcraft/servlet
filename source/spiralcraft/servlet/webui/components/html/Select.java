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
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.AbstractSelectControl;

/**
 * <P>A standard HTML SELECT list, bound to a target and a source.
 * </P>
 * 
 * <P>The "x" (binding target) property contains an expression that references
 *   the currently selected item(s). The optional "source" property contains an 
 *   expression that provides a list of candidate values, used for generating 
 *   the set of options (see the Option class).
 * </P>
 *  
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
public class Select<Ttarget,Tvalue>
  extends AbstractSelectControl<Ttarget,Tvalue>
{

  
  private Tag tag=new Tag();
  
  private class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return "select";
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<?> state=getState(context);
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      if (multi)
      { renderAttribute(context.getWriter(),"multiple",null);
      }
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { Select.super.render(context);
    }    
    
  };

  private ErrorTag errorTag
    =new ErrorTag(tag);
  
  public Tag getTag()
  { return tag;
  }
  



  
  @Override
  public void render(EventContext context)
    throws IOException
  {
    ControlState<?> state=getState(context);
    if (state.isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
    state.setPresented(true);
  }
  
//  void setValueConverter(StringConverter<Tvalue> converter)
//  { this.valueConverter=converter;
//  }
  
  @Override
  public Focus<?> bindExports()
    throws BindException
  { 
    Focus<?> focus=super.bindExports();
    tag.bind(getFocus());
    errorTag.bind(getFocus());
    return focus;
  }   


}
  

