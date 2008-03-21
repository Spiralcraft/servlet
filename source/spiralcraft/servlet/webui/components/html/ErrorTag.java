package spiralcraft.servlet.webui.components.html;

import java.io.IOException;
import java.io.Writer;

import spiralcraft.servlet.webui.ControlState;
import spiralcraft.textgen.EventContext;

public class ErrorTag
    extends AbstractTag
{
  private AbstractTag controlTag;
  private String tagName="box";
  
  public ErrorTag(AbstractTag controlTag)
  { this.controlTag=controlTag;
  }

  @Override
  protected String getTagName(EventContext context)
  { return tagName;
  }
    
  protected boolean hasContent()
  { return true;
  }
    

  protected void renderContent(EventContext context)
    throws IOException
  { 
    ControlState<?> state=(ControlState<?>) context.getState();
    Writer out=context.getWriter();
    
    if (state.getError()!=null)
    { out.write(state.getError());
    }
    if (state.getException()!=null)
    { 
      out.write("<!--\r\n");

      Throwable exception=state.getException();
      while (exception!=null)
      {
        out.write(exception.toString());

        for (StackTraceElement element : exception.getStackTrace())
        {
          out.write(element.toString());
          out.write("\r\n");
        }
        exception=exception.getCause();
      }
      out.write("-->\r\n");
      
    }
    if (controlTag!=null)
    { controlTag.render(context);
    }
    
  }


  protected void renderAttributes(EventContext context)
    throws IOException
  { 
    context.getWriter().write(" color=\"red\"");
    super.renderAttributes(context);
  }
  

}
