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

import java.io.IOException;
import java.net.URI;

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.servlet.webui.Component;
import spiralcraft.text.MessageFormat;
import spiralcraft.textgen.OutputContext;

public class ScriptTag
  extends AbstractTag
{
  
  private MessageFormat code;
  private URI[] requiredScripts;
  private String type;
  
  { addNewLine=true;
  }
  
  public ScriptTag()
  {
  }
  
  public ScriptTag(MessageFormat code)
  { 
    this.code=code;
    setType("text/javascript");
  }
  
  public void setType(String type)
  { this.type=type;
  }
  
  public void setSrc(String value)
  { appendAttribute("src",value);
  }

  public void setCode(MessageFormat code)
  { this.code=code;
  }
  
  public void setRequiredScripts(URI[] requiredScripts)
  { this.requiredScripts=requiredScripts;
  }
  
  @Override
  public Focus<?> bind(Focus<?> focus)
    throws ContextualException
  { 
    if (code!=null)
    { code.bind(focus);
    }
    if (requiredScripts!=null)
    {
      Component component=LangUtil.assertInstance(Component.class,focus);
      Page page=component.findComponent(Page.class);
      if (page!=null)
      {
        for (URI script:requiredScripts)
        { page.requireScript(script);
        }
      }
    }    
    if (type==null)
    { type="text/javascript";
    }
    appendAttribute("type",type);
    return super.bind(focus);
  }
  
  @Override
  protected String getTagName(Dispatcher dispatcher)
  { return "script";
  }

  @Override
  protected boolean hasContent()
  { return true;
  }  
  
 
  @Override
  protected void renderContent
    (Dispatcher dispatcher,Message message,MessageHandlerChain next)
    throws IOException
  { 
    if (code!=null)
    { code.render(OutputContext.get());
    }
    super.renderContent(dispatcher,message,next);
  }

}