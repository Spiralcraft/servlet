package spiralcraft.servlet.webui.components.html;

import java.io.IOException;

import spiralcraft.servlet.webui.ServiceContext;

public class Editor
    extends spiralcraft.servlet.webui.components.Editor
{

  @Override
  protected void renderError(ServiceContext context) throws IOException
  { new ErrorTag(null).render(context);
  }

}
