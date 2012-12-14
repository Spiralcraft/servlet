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

import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.text.MessageFormat;
import spiralcraft.textgen.OutputContext;

import spiralcraft.json.ToJson;

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
  
  private MessageFormat onRegisterJS;
  private MessageFormat onBodyLoadJS;
  private String[] events;
  private Binding<Object> dataX;
  private Channel<String> toJson;
  
  {
    tagPosition=-1;
    setCode(MessageFormat.create("{}"));
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
  
  /**
   * A MessageFormat that should render to literal JSON as the "data" property
   *   of the peer binding.
   * 
   * @param js
   */
  public void setDataJS(MessageFormat js)
  { this.setCode(js);
  }
  
  /**
   * <p>
   * An arbitrary expression that will be rendered to JSON as the "data"
   *   property of the peer binding. 
   * </p>
   *   
   * <p>This property will override the dataJS property if both are set
   * </p>
   * 
   * @param dataX
   */
  public void setDataX(Binding<Object> dataX)
  { 
    this.dataX=dataX;
    this.setCode(null);
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
    if (dataX!=null)
    { 
      dataX.bind(focus);
      toJson=new ToJson<Object>().bindChannel(dataX,focus,null);
    }
    return super.bind(focus);
  }
  
  
  @Override
  protected void renderContent
    (Dispatcher dispatcher,Message message,MessageHandlerChain next)
    throws IOException
  { 
    String id=((ComponentState) dispatcher.getState()).getId();
    String peerFunction="$SC('"+id+"')";
    
    Appendable out=OutputContext.get();
    out.append(peerFunction);
    out.append(".setData(");
    if (toJson!=null)
    { out.append(toJson.get());
    }
    super.renderContent(dispatcher,message,next);
    out.append(");\r\n");
        
    
    if (onRegisterJS!=null)
    { 
      out.append("(");
      addSelfFunctionDef(out,onRegisterJS);
      out.append(").call(");
      out.append(peerFunction);
      out.append(",");
      out.append(peerFunction);
      out.append(");\r\n");
    }
    
    
    if (onBodyLoadJS!=null)
    { 
      out.append(peerFunction);
      out.append(".attachBodyOnLoad(");
      addSelfFunctionDef(out,onBodyLoadJS);
      out.append(");\r\n");
    }
    
    
    if (events!=null)
    { 
      out.append(peerFunction);
      out.append(".setEvents(");
      out.append(makeEventObject());
      out.append(");\r\n");
    }
    
  }
  
  private String makeEventObject()
  {
    StringBuilder out=new StringBuilder();
    out.append("[");
    boolean first=true;
    for (String eventName:events)
    { 
      if (!first)
      { out.append(",");
      }
      else
      { first=false;
      }
      out.append("'"+eventName+"'");      
    }
    out.append("]");
    return out.toString();
  }
  
  private void addSelfFunctionDef(Appendable out,MessageFormat body)
    throws IOException
  {
    out.append("function(self) {\r\n");
    body.render(out);
    out.append("\r\n}");
  }
  

}