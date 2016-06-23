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

import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.components.html.kit.AbstractHtmlTextInput;
import spiralcraft.servlet.webui.components.html.kit.AbstractHtmlTextInput.TextTag;
import spiralcraft.time.TimeField;
import spiralcraft.util.string.DateToString;
import spiralcraft.util.string.StringConverter;

public class DateInput
  extends AbstractHtmlTextInput<Date>
{
  
  private String format;
  private TimeField precision=TimeField.MILLISECOND;
  private boolean roundUp=false;
  private String inputType="text";
  private boolean useNativePicker;
  
  @Override
  protected StringConverter<Date> createConverter(Focus<?> focus)
  {
    if (useNativePicker && format==null)
    { format="yyyy-MM-dd";
    }
    if (format==null)
    { return null;
    }
    return new DateToString(format,precision,roundUp);
  }
  
  
  public void setPrecision(TimeField precision)
  { this.precision=precision;
  }
  
  public void setRoundUp(boolean roundUp)
  { this.roundUp=roundUp;
  }

  @Override
  protected TextTag createTag()
  { return new Tag();
  }
    
  public void setFormat(String format)
  { this.format=format;
  }
  
  public void setUseNativePicker(boolean useNativePicker)
  { this.useNativePicker=useNativePicker; 
  }
  
  public class Tag 
    extends TextTag
  {    
    { addStandardClass("sc-webui-date-input");
    }
  
    @Override
    protected String getInputType()
    { return useNativePicker?"date":inputType;
    }

  }  
  
}

