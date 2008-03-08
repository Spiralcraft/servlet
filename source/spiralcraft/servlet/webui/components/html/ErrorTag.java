package spiralcraft.servlet.webui.components.html;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.EventContext;

public class ErrorTag
    extends AbstractTag
{
  private AbstractTag controlTag;
  private String tagName="div";
  
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
      out.write(state.getException().toString());
      out.write("<!--\r\n");
      for (StackTraceElement element : state.getException().getStackTrace())
      {
        out.write(element.toString());
        out.write("\r\n");
      }
      out.write("-->\r\n");
      
    }
    if (controlTag!=null)
    { controlTag.render(context);
    }
    
  }


  

}
