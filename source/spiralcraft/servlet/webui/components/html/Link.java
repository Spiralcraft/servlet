package spiralcraft.servlet.webui.components.html;

import spiralcraft.textgen.EventContext;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;

import spiralcraft.util.ArrayUtil;

import java.io.IOException;

public class Link
  extends AbstractTag<Void>
{

  private String actionName;
  
  public void setActionName(String actionName)
  { this.actionName=actionName;
  }
  
  @Override
  protected String getTagName(EventContext context)
  { return "a";
  }

  @Override
  protected void renderAttributes(EventContext context)
    throws IOException
  { 
    
    String actionURI
      =((ServiceContext) context)
        .registerAction(createAction(context),actionName);
    
    
    renderAttribute(context.getWriter(),"href",actionURI);
  }
  

  
  protected Action createAction(EventContext context)
  {
    return new Action(getPath())
    {
      public void invoke(ServiceContext context)
      { 
        System.err.println
          ("Link: Generic action invoked: "
          +ArrayUtil.format(getTargetPath(),"/",null)
          );
      }
    };
  }
}
