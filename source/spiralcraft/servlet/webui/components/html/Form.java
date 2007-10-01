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
import spiralcraft.textgen.Message;
import spiralcraft.textgen.InitializeMessage;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.ControlMessage;

import spiralcraft.util.ArrayUtil;



import java.io.IOException;
import java.util.LinkedList;

public class Form<T>
  extends ControlGroup<T>
{

  private String actionName;
  
  private final AbstractTag tag=new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "form";
    }
    
    protected boolean hasContent()
    { return true;
    }
    
    protected void renderContent(EventContext context)
      throws IOException
    { Form.super.render(context);
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    { 
      
      String actionURI
        =((ServiceContext) context)
          .registerAction(createAction(context),actionName);
      
      
      renderAttribute(context.getWriter(),"action",actionURI);
      renderAttribute(context.getWriter(),"method","POST");
    }
  };
  
  public void setActionName(String actionName)
  { this.actionName=actionName;
  }
  
  protected Action createAction(EventContext context)
  {
    return new Action(context.getState().getPath())
    {
      @SuppressWarnings("unchecked") // Blind cast
      public void invoke(ServiceContext context)
      { 
//        System.err.println
//          ("Form: Generic action invoked: "
//          +ArrayUtil.format(getTargetPath(),"/",null)
//          );

        FormState<T> formState
          =(FormState<T>) context.getState();
        
        message(context,ControlMessage.GATHER_MESSAGE,null);
        
        if ( !formState.isErrorState())
        { 
          // XXX Provide more control over rescatter
          message(context,ControlMessage.SCATTER_MESSAGE,null);
        }
      }
    };
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { tag.render(context);
  }
  
  @Override
  public FormState<T> createState()
  { return new FormState<T>(this);
  }
  
  
  public class FormState<X>
    extends ControlGroupState<X>
  {
    public FormState(Form<X> form)
    { super(form);
    }
  }
}
