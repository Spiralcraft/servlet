package spiralcraft.servlet.webui.components.html;

import java.io.IOException;




import spiralcraft.data.DataComposite;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.textgen.EventContext;

public class SelectionEditor<TorigContent extends DataComposite,TselectItem>
    extends spiralcraft.servlet.webui.components.SelectionEditor<TorigContent,TselectItem>
{

  private final AbstractTag tag=new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "div";
    }
    
    protected boolean hasContent()
    { return true;
    }
    
    protected void renderContent(EventContext context)
      throws IOException
    { SelectionEditor.super.render(context);
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    { super.renderAttributes(context);
    }
  };
  
  private ErrorTag errorTag
    =new ErrorTag(tag);

  public AbstractTag getTag()
  { return tag;
  }
  
  public AbstractTag getErrorTag()
  { return errorTag;
  }

  public void render(EventContext context)
    throws IOException
  { 
    if ( ((ControlState<?>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }

  protected Focus<?> bindSelf()
    throws BindException
  {
    if (findElement(Form.class)==null)
    { throw new BindException("Editor must be contained in a Form");
    }
    return super.bindSelf();
  }
  

}
