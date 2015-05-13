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


import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.kit.AbstractMessageHandler;
import spiralcraft.common.ContextualException;
import spiralcraft.servlet.webui.Component;
import spiralcraft.text.MessageFormat;
import spiralcraft.textgen.OutputContext;
import spiralcraft.textgen.PrepareMessage;
import spiralcraft.textgen.RenderMessage;

public class Script
  extends Component
{

  public static enum Target
  {
    HEAD
    {
      @Override
      public Appendable get(Page page)
      { return page.getHead();
      }
    }
    ,BODY
    {
      @Override
      public Appendable get(Page page)
      { return page.getStartOfBody();
      }
    }
    ,FOOT
    {
      @Override
      public Appendable get(Page page)
      { return page.getEndOfBody();
      }
    }
    ;
    
    public abstract Appendable get(Page page);
  }
  
  private Target target;
  private boolean targetOptional;
  private ScriptTag scriptTag
    =new ScriptTag();
  private Page page;
  
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
  
  
  class PrepareHandler
      extends AbstractMessageHandler
  {
    { this.type=PrepareMessage.TYPE;
    }

    @Override
    protected void doHandler(Dispatcher dispatcher, Message message,
            MessageHandlerChain next) 
    { 
      next.handleMessage(dispatcher, message);
      
      if (target!=null)
      {
        Appendable out=target.get(page);
        if (out!=null)
        {
          OutputContext.push(target.get(page));
          try
          { next.handleMessage(dispatcher,RenderMessage.INSTANCE);
          }
          finally
          { OutputContext.pop();
          }
        }
        else
        { log.warning(getDeclarationInfo()+": Output target is null");
        }
      }
      else
      { log.warning(getDeclarationInfo()+": No output target defined");
      }
      
    }
  }
  
  class RenderHandler
    extends AbstractMessageHandler
  { 
    { this.type=RenderMessage.TYPE;
    }
    
    @Override
    protected void doHandler(Dispatcher dispatcher, Message message,
            MessageHandlerChain next) 
    { // Don't render in-context
    }
  }
  
  
  @Override
  protected void addHandlers() 
    throws ContextualException
  {
    
    if (target!=null)
    {
      page=this.findComponent(Page.class);
      if (page!=null)
      {
        addHandler(new RenderHandler());
        addHandler(new PrepareHandler());
        addHandler(scriptTag);
      }
      else if (targetOptional)
      { addHandler(scriptTag);
      }
      else
      { 
        throw new ContextualException
          ("Script tag could not find a containing Page to target"
          ,getDeclarationInfo()
          );
      }
    }
    else
    { addHandler(scriptTag);
    }
    
    super.addHandlers();
    
  }
}
