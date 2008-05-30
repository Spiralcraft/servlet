package spiralcraft.servlet.webui.components.html;

import java.io.IOException;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.textgen.EventContext;

public class Login
    extends spiralcraft.servlet.webui.components.Login
{

  private final AbstractTag tag=new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "SPAN";
    }
    
    protected boolean hasContent()
    { return true;
    }
    
    protected void renderContent(EventContext context)
      throws IOException
    { Login.super.render(context);
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

  protected Focus<?> bindExports()
    throws BindException
  {
    if (findElement(Form.class)==null)
    { throw new BindException("Login must be contained in a Form");
    }
    return super.bindExports();
  }
  

}
