//
//Copyright (c) 1998,2007 Michael Toth
//Spiralcraft Inc., All Rights Reserved
//
//This package is part of the Spiralcraft project and is licensed under
//a multiple-license framework.
//
//You may not use this file except in compliance with the terms found in the
//SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
//at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
//Unless otherwise agreed to in writing, this software is distributed on an
//"AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.servlet.webui.components.html;

import spiralcraft.textgen.EventContext;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.Component;

import spiralcraft.util.ArrayUtil;

import java.io.IOException;

public class Link
  extends Component
{

  private String actionName;
  private AbstractTag tag=new AbstractTag()
  {
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

    @Override
    protected boolean hasContent()
    { return getChildCount()>0;
    }
    
    @Override
    protected void renderContent(EventContext context)
      throws IOException
    { 
      Link.super.render(context);
    }
    
  };
  
  
  public void setActionName(String actionName)
  { this.actionName=actionName;
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  {
    tag.render(context);
  }
  

  
  protected Action createAction(EventContext context)
  {
    return new Action(context.getState().getPath())
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
