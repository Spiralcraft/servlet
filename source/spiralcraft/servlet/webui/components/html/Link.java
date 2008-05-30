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

import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.command.Command;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.Component;

import spiralcraft.util.ArrayUtil;

import java.io.IOException;
import java.util.List;

public class Link
  extends Component
{

  private String actionName;
  
  private Expression<Command<?,?>> commandExpression;
  private Channel<Command<?,?>> commandChannel;
  
  public void setX(Expression<Command<?,?>> expression)
  { commandExpression=expression;
  }
  
  private AbstractTag tag=new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "A";
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    { 
      String effectiveActionName=null;
      if (actionName!=null)
      { effectiveActionName=actionName;
      }
      else
      { 
        effectiveActionName
          =ArrayUtil.format(context.getState().getPath(),".","");
      }
      
      String actionURI
        =((ServiceContext) context)
          .registerAction(createAction(context),effectiveActionName);
      
      
      renderAttribute(context.getWriter(),"href",actionURI);
      super.renderAttributes(context);
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
  
    @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    
    Focus<?> parentFocus=getParent().getFocus();
    if (commandExpression!=null)
    { commandChannel=parentFocus.bind(commandExpression);
    }

    bindChildren(childUnits);
    
  }
  
  public AbstractTag getTag()
  { return tag;
  }
  
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
      Command<?,?> command=commandChannel!=null?commandChannel.get():null;
      
      public void invoke(ServiceContext context)
      { command.execute();
      }
    };
  }
}
