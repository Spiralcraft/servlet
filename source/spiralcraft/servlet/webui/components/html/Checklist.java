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
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.AbstractSelectControl;

/**
 * <p>A group of checkboxes bound to a single location for multiple selections
 * </p>
 * 
 * <p>The "x" (binding target) property contains an expression that references
 *   the currently selected item(s). The optional "source" property contains an 
 *   expression that provides a list of candidate values, used for generating 
 *   the set of options (see the RadioButton class).
 * </p>
 *  
 * @author mike
 *
 * @param <Ttarget>
 * @param <Tvalue>
 */
public class Checklist<Ttarget,Tvalue>
  extends AbstractSelectControl<Ttarget,Tvalue>
{

  { multi=true;
  }
  
  private Tag tag=new Tag();
  
  public class Tag
    extends AbstractTag
  {
    private String tagName=null;
    
    @Override
    protected String getTagName(Dispatcher context)
    { return tagName;
    }
    
    public void setTagName(String tagName)
    { this.tagName=tagName;
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
    protected void renderAfter
      (Dispatcher context)
      throws IOException
    { 
      super.renderAfter(context);
      ((ControlState<?>) context.getState()).setPresented(true);
    }
  };

  private ErrorTag errorTag
    =new ErrorTag();
  
  { 
    addHandler(errorTag);
    addHandler(tag);
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }

}
  

