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

import spiralcraft.servlet.webui.components.html.kit.AbstractHtmlTextInput;

public class TextInput<Ttarget>
  extends AbstractHtmlTextInput<Ttarget>
{
  
  private boolean password;
  
  
  /**
   * Whether the control is in password mode
   * 
   * @param password
   */
  public void setPassword(boolean password)
  { this.password=password;
  }
  
  
  @Override
  protected TextTag createTag()
  { return new Tag();
  }
  
  public class Tag 
    extends TextTag
  {    
    { addStandardClass("sc-webui-text-input");
    }
    
    @Override
    protected String getInputType()
    { return password?"password":"text";
    }
    
    @Override
    protected boolean shouldRenderValue()
    { return !password;
    }
  }
  
}

