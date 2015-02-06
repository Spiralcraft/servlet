package spiralcraft.servlet.webui.components.html.kit;

import spiralcraft.servlet.webui.ComponentState;

public interface ServerPeer
{
  ComponentState getState();
  
  String getCSID();
}
