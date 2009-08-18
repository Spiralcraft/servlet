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
 * <p>A group of radio buttons bound to a single location
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
public class RadioGroup<Ttarget,Tvalue>
  extends AbstractSelectControl<Ttarget,Tvalue>
{

  
  private Tag tag=new Tag();
  
  public class Tag
    extends AbstractTag
  {
    private String tagName=null;
    
    @Override
    protected String getTagName(EventContext context)
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
    protected void renderContent(EventContext context)
      throws IOException
    { RadioGroup.super.render(context);
    }    
    
  };

  private ErrorTag errorTag
    =new ErrorTag(tag);
  
  public Tag getTag()
  { return tag;
  }
  



  
  @Override
  @SuppressWarnings("unchecked")
  public void render(EventContext context)
    throws IOException
  {
    if (((ControlState<Ttarget>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
    ((ControlState<?>) context.getState()).setPresented(true);
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
  

