//
//Copyright (c) 2012 Michael Toth
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

import spiralcraft.app.Dispatcher;
import spiralcraft.text.MessageFormat;

public class LinkTag
  extends AbstractTag
{ 

  { 
    tagPosition=-1;
    addNewLine=true;
  }
  
  public void setRel(String value)
  { this.appendAttribute("rel",value);
  }
  
  public void setRev(String value)
  { this.appendAttribute("rev",value);
  }

  public void setHREF(MessageFormat value)
  { this.addStandardBinding("href",value);
  }
  
  public void setMedia(String value)
  { this.appendAttribute("media",value);
  }
  
  public void setType(String value)
  { this.appendAttribute("type",value);
  }


  @Override
  protected String getTagName(Dispatcher dispatcher)
  { return "link";
  }
  
  @Override
  protected boolean hasContent()
  { return false;
  }
}