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

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.common.ContextualException;

import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.text.MessageFormat;
import spiralcraft.textgen.OutputContext;

/**
 * Binds a client-side javascript object to the Component that contains
 *   this handler
 * 
 * @author mike
 *
 */
public class PeerJSTag
  extends ScriptTag
{
  
  private String registerJSFunction="SPIRALCRAFT.webui.bindPeer";
  private MessageFormat onRegisterJS;
  private MessageFormat onBodyLoadJS;
  private String[] events;
  
  { tagPosition=-1;
  }

  
  public PeerJSTag(MessageFormat dataObjectCode)
  { super(dataObjectCode);
  }
  
  public PeerJSTag()
  {
  }
  
  public void setEvents(String[] events)
  { this.events=events;
  }
  
  public void setDataJS(MessageFormat js)
  { this.setCode(js);
  }
  
  public void setOnRegisterJS(MessageFormat js)
  { this.onRegisterJS=js;
  }
  
  public void setOnBodyLoadJS(MessageFormat js)
  { this.onBodyLoadJS=js;
  }
   
  @Override
  public Focus<?> bind(Focus<?> focus)
    throws ContextualException
  {
    if (onRegisterJS!=null)
    { onRegisterJS.bind(focus);
    }
    if (onBodyLoadJS!=null)
    { onBodyLoadJS.bind(focus);
    }

    return super.bind(focus);
  }
  
  @Override
  protected void renderContent
    (Dispatcher dispatcher,Message message,MessageHandlerChain next)
    throws IOException
  { 
    Appendable out=OutputContext.get();
    out.append(registerJSFunction);
    out.append("({");
    out.append(" id: ");
    out.append("\"");
    out.append( ((ComponentState) dispatcher.getState()).getId());
    out.append("\"");
    out.append(",\r\n");
    
    out.append("element: function() {return SPIRALCRAFT.webui.getElement(this.id);},\r\n");
    
    if (onRegisterJS!=null)
    { 
      addMethod(out,"onRegister",onRegisterJS);
      out.append(",\r\n");
    }
    if (onBodyLoadJS!=null)
    { 
      addMethod(out,"onBodyLoad",onBodyLoadJS);
      out.append(",\r\n");
    }
    
    
    if (events!=null)
    { 
      addField(out,"events",makeEventObject());
      out.append(",\r\n");
    }
    
    out.append(" data: ");
    super.renderContent(dispatcher,message,next);
    out.append("\r\n})\r\n");
  }
  
  private String makeEventObject()
  {
    StringBuilder out=new StringBuilder();
    out.append("  {\r\n");
    boolean first=true;
    for (String eventName:events)
    { 
      out.append("    ");
      if (!first)
      { out.append(",");
      }
      else
      { first=false;
      }
      out.append(eventName+": new SPIRALCRAFT.webui.dispatcher()");
      
    }
    out.append("\r\n  }");
    return out.toString();
  }
  
  
  private void addField(Appendable out,String name,String body)
    throws IOException
  { 
    out.append(name);
    out.append(": ");
    out.append(body);
  }

  
  private void addMethod(Appendable out,String name,MessageFormat body)
    throws IOException
  { 
    out.append(name);
    out.append(": ");
    out.append("function(self) {\r\n");
    body.render(out);
    out.append("\r\n}");
  }
  
  private void addMethod(Appendable out,String name,String body)
    throws IOException
  { 
    out.append(name);
    out.append(": ");
    out.append("function(self) {\r\n");
    out.append(body);
    out.append("\r\n}");
  }

}