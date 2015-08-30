package spiralcraft.servlet.webui;

import spiralcraft.app.StateFrame;
import spiralcraft.servlet.webui.kit.PortSession;

public class ServiceRootComponentState
  extends ComponentState
{

  boolean dirty;
  
  private volatile PortSession portSession;
  
  public ServiceRootComponentState(Component component)
  { super(component);
  }
  
  public synchronized PortSession getPortSession(ServiceContext context)
  {
    if (portSession==null)
    { 
      PortSession parentSession=context.getPortSession();
      PortSession portSession;
      if (parentSession!=null)
      {
        portSession=new PortSession(parentSession);
        portSession.setLocalURI(parentSession.getLocalURI());
      }
      else
      {
        portSession=new PortSession();
      }
      portSession.setState(this);
      portSession.setPort(getPath());
      portSession.setPortId(getId());
      this.portSession=portSession;
    }
    return portSession;
  }
  
  public boolean isDirty()
  { return dirty;
  }
  
  @Override
  public void enterFrame(StateFrame frame)
  { 
    super.enterFrame(frame);
    if (isNewFrame())
    { dirty=false;
    }
    if (isNewFrame() && portSession!=null)
    { portSession.setFrame(frame);
    }
  }
  
    
}
