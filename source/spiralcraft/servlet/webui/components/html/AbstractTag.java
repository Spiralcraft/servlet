package spiralcraft.servlet.webui.components.html;

import spiralcraft.textgen.EventContext;

import spiralcraft.servlet.webui.Component;

import java.io.IOException;
import java.io.Writer;

import spiralcraft.text.html.AttributeEncoder;

public abstract class AbstractTag<T>
  extends Component<T>
{
  private static final AttributeEncoder attributeEncoder
    =new AttributeEncoder();
    
  protected abstract String getTagName(EventContext context);
  
  protected void renderAttribute(Writer writer,String name,String value)
    throws IOException
  {
    writer.write(name);
    writer.write("=\"");
    attributeEncoder.encode(value,writer);
    writer.write("\" ");
  }
  
  protected void renderAttributes(EventContext context)
    throws IOException
  {
  }
  
  protected void renderContent(EventContext context)
    throws IOException
  { renderChildren(context);
  }
  
  @Override
  public final void render(EventContext context)
    throws IOException
  { 
    Writer writer=context.getWriter();
    writer.write("<");
    writer.write(getTagName(context));
    writer.write(" ");
    
    renderAttributes(context);
    
    if (hasChildren())
    { 
      writer.write(">");
   
      renderContent(context);
    
      writer.write("</");
      writer.write(getTagName(context));
      writer.write(">");
    }
    else
    { writer.write("/>");
    }
  }
}
