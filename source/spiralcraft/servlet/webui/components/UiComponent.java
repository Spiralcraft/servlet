package spiralcraft.servlet.webui.components;

import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.time.Clock;

import spiralcraft.textgen.Element;
import spiralcraft.textgen.RenderingContext;

import java.io.IOException;

/**
 * The root of a WebUI component tree.
 * 
 * The UiComponent is the Component which is addressed directly via the HTTP
 *   client and provides the UI with some control over the HTTP interaction.
 * 
 * @author mike
 *
 */
public class UiComponent
  extends Component
{
  
  private final Element element;
  
  public UiComponent(Element element)
  { this.element=element;
  }
  
  public String getContentType()
  { return "text/html";
  }
  
  public long getLastModified()
  { return Clock.instance().approxTimeMillis();
  }

  @Override
  public void render(ServiceContext context)
    throws IOException
  {
    System.err.println("UiComponent: render");
    RenderingContext tglContext=new RenderingContext(context.getWriter());
    element.write(tglContext);
    tglContext.getWriter().flush();
  }

}
