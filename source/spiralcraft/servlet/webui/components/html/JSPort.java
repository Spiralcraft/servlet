package spiralcraft.servlet.webui.components.html;

import java.io.IOException;

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ComponentState;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.Port;
import spiralcraft.textgen.OutputContext;

public class JSPort
  extends Port
{

  private PortTag tag=new PortTag();
  

  
  public class PortTag 
    extends ScriptTag
  {
  
    { tagPosition=-1;
    }
    @Override
    public Focus<?> bind(Focus<?> focus)
      throws ContextualException
    { 
      
      focus=super.bind(focus);
      
      return focus;
    }
    
    
    @Override
    protected boolean shouldHandleMessage(Dispatcher dispatcher,Message message)
    {
      return portCall.get()!=Boolean.TRUE
        && super.shouldHandleMessage(dispatcher,message);
    }
    
    @Override
    protected void renderContent
      (Dispatcher dispatcher,Message message,MessageHandlerChain next)
      throws IOException
    { 
      ComponentState state=(ComponentState) dispatcher.getState();
      ComponentState parentState=(ComponentState) state.getParent();
      OutputContext.get()
        .append("$SC(\"")
        .append(parentState.getId())
        .append("\").")
        .append(getId())
        .append("=new SPIRALCRAFT.webui.Port(\"")
        .append(((ServiceContext) dispatcher)
          .registerPort(state.getId(),state.getPath()))
        .append("\");");
        ;
      super.renderContent(dispatcher,message,next);
    }    
  }
  
  @Override
  protected void addExternalHandlers()
    throws ContextualException
  {
    
    super.addExternalHandlers();
    addHandler(tag);
  }
}
