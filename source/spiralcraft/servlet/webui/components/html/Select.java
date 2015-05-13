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
import spiralcraft.common.ContextualException;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.kit.AbstractSelectControl;

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
    protected String getTagName(Dispatcher dispatcher)
    { return "select";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      ControlState<?> state=getState(context);
      renderAttribute(out,"name",state.getVariableName());
      if (multi)
      { renderAttribute(out,"multiple",null);
      }
      state.setPresented(true);
      super.renderAttributes(context,out);
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
      
    
  };

  private ErrorTag errorTag
    =new ErrorTag();
  
  @Override
  protected void addHandlers()
    throws ContextualException
  { 
    addHandler(errorTag);
    addHandler(tag);
    FormField<?> formField=this.findComponent(FormField.class);
    if (formField!=null)
    { 
      addHandler(formField.newInputHandler());
      tag.setGenerateId(true);
    }    
    super.addHandlers();
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public void setMulti(boolean multi)
  { this.multi=multi;
  }

}
  

