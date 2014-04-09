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

import java.util.ArrayList;
import java.util.List;

import spiralcraft.app.Scaffold;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.Component;
import spiralcraft.text.MessageFormat;
import spiralcraft.textgen.compiler.ContentUnit;
import spiralcraft.textgen.compiler.TglUnit;

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
  private boolean targetInstalled;
  
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
  protected List<Scaffold<?>> expandChildren(Focus<?> focus,List<TglUnit> children)
    throws ContextualException
  { 
    if (targetInstalled)
    { 
      // Make sure script code renders in target and not locally

      // TODO: Make this mechanism more generic by allowing ElementUnits to
      //   specify a "default" property" to accept content.
      if (children!=null && children.size()==1 && children.get(0) instanceof ContentUnit)
      { 
        ContentUnit content=(ContentUnit) children.get(0);
        String text=content.getContent().toString().trim();
        if (!text.isEmpty())
        { 
          if (scriptTag.getCode()!=null)
          { throw new ContextualException("Code for Script element is already defined");
          }
          MessageFormat mf=new MessageFormat(text);
          scriptTag.setCode(mf);
        }
      }
      return new ArrayList<Scaffold<?>>();
    }
    return super.expandChildren(focus,children);
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
        
        this.targetInstalled=true;
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
