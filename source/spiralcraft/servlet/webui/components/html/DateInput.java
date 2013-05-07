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

import java.util.Date;

import spiralcraft.servlet.webui.components.html.kit.AbstractHtmlTextInput;
import spiralcraft.util.string.DateToString;

public class DateInput
  extends AbstractHtmlTextInput<Date>
{
  
  @Override
  protected TextTag createTag()
  { return new TextTag();
  }
    
  public void setFormat(String format)
  { this.setConverter(new DateToString(format));
  }
  
}

