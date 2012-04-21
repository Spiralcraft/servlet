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
package spiralcraft.servlet.webui.components.html;

import spiralcraft.common.ContextualException;
import spiralcraft.servlet.webui.Component;
import spiralcraft.text.MessageFormat;

public class Script
  extends Component
{

  public static enum Target
  {
    HEAD
    ,BODY
    ,FOOT
  }
  
  private Target target;
  private boolean targetOptional;
  private ScriptTag scriptTag
    =new ScriptTag();
  
  public void setCode(MessageFormat code)
  { scriptTag.setCode(code);
  }
  
  public void setTarget(Target target)
  { this.target=target;
  }
  
  public void setTargetOptional(boolean targetOptional)
  { this.targetOptional=targetOptional;
  }
  
  public void setSrc(String src)
  { scriptTag.setSrc(src);
  }
  
  public void setType(String type)
  { scriptTag.setType(type);
  }
  
  @Override
  protected void addHandlers() 
    throws ContextualException
  {
    
    if (target!=null)
    {
      Page page=this.findComponent(Page.class);
      if (page!=null)
      { 
        switch (target)
        {
          case HEAD:
            scriptTag.setTagPosition(-1);
            page.addTagToHead(scriptTag);
            break;
          case BODY:
            scriptTag.setTagPosition(-1);
            page.addTagToBody(scriptTag);
            break;
          case FOOT:
            scriptTag.setTagPosition(1);
            page.addTagToBody(scriptTag);
            break;
        }
      }
      else if (!targetOptional)
      { throw new ContextualException("No Page exists in context");
      }
      else
      { addHandler(scriptTag);
      }
    }
    else
    { addHandler(scriptTag);
    }
    
    super.addHandlers();
    
  }
}
