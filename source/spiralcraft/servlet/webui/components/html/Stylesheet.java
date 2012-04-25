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

/**
 * A stylesheet link element
 * @author mike
 *
 */
public class Stylesheet
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
  private LinkTag linkTag
    =new LinkTag();
  { 
    linkTag.setType("text/css");
    linkTag.setRel("stylesheet");
  }
  
  
  public void setTarget(Target target)
  { this.target=target;
  }
  
  
  public void setHref(String href)
  { linkTag.setHREF(href);
  }
  

  public void setMedia(String media)
  { linkTag.setMedia(media);
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
            linkTag.setTagPosition(-1);
            page.addTagToHead(linkTag);
            break;
          case BODY:
            linkTag.setTagPosition(-1);
            page.addTagToBody(linkTag);
            break;
          case FOOT:
            linkTag.setTagPosition(1);
            page.addTagToBody(linkTag);
            break;
        }
      }
      else if (!targetOptional)
      { throw new ContextualException("No Page exists in context");
      }
      else
      { addHandler(linkTag);
      }
    }
    else
    { addHandler(linkTag);
    }
    
    super.addHandlers();
    
  }
}
