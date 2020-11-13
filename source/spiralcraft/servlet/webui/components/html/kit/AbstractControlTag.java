package spiralcraft.servlet.webui.components.html.kit;

import java.io.IOException;

import spiralcraft.app.Dispatcher;
import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.html.AbstractTag;

public abstract class AbstractControlTag
  extends AbstractTag
{
  protected String errorStateClass="sc-webui-is-error";
  protected final Control<?> control;
  
  protected AbstractControlTag(Control<?> control)
  { this.control=control;
  }
  
  public void setErrorStateClass(String errorStateClass)
  { this.errorStateClass=errorStateClass;
  }
  
  protected void renderStatefulClasses(Dispatcher context,Appendable out)
    throws IOException
  {       
  
    ControlState<?> state=control.getState();
    if (state.isErrorState())
    { attributeEncoder.encode(errorStateClass,out);
    }
    super.renderStatefulClasses(context, out);
  }

}
