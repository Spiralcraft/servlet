package spiralcraft.servlet.webui.components.html;

import java.io.IOException;



import spiralcraft.servlet.webui.ControlState;
import spiralcraft.textgen.EventContext;

public class Editor
    extends spiralcraft.servlet.webui.components.Editor
{

  private ErrorTag errorTag
    =new ErrorTag(null)
  {
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { 
      super.renderContent(context);
      Editor.super.render(context);
    }
  };
  
  public void render(EventContext context)
    throws IOException
  { 
    if ( ((ControlState<?>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { super.render(context);
    }
  }

  

}
