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

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLogger;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.CommandMessage;
import spiralcraft.servlet.webui.GatherMessage;

import spiralcraft.util.ArrayUtil;

import spiralcraft.command.Command;


import java.io.IOException;

public class Form<T>
  extends ControlGroup<T>
{
  private static final ClassLogger log=ClassLogger.getInstance(Form.class);
  
  private static final GatherMessage GATHER_MESSAGE=new GatherMessage();
  private static final CommandMessage COMMAND_MESSAGE=new CommandMessage();
  
  private boolean mimeEncoded;
  
  private Expression<Command<?,?>> onPost;
  private Channel<Command<?,?>> onPostChannel;
  
  private final AbstractTag tag=new AbstractTag()
  {
    @Override
    protected String getTagName(EventContext context)
    { return "form";
    }
    
    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
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
          .registerAction(createAction(context));
      
      
      renderAttribute(context.getWriter(),"action",actionURI);
      renderAttribute(context.getWriter(),"method","POST");
      if (mimeEncoded)
      { renderAttribute(context.getWriter(),"enctype","multipart/form-data");
      }
      super.renderAttributes(context);
    }
  };
  
  private final ErrorTag errorTag=new ErrorTag(tag);
  
  public void setOnPost(Expression<Command<?,?>> onPost)
  { this.onPost=onPost;
  }
  
  public void setMimeEncoded(boolean mimeEncoded)
  { this.mimeEncoded=mimeEncoded;
  }
  
  @Override
  public String getVariableName()
  { return null;
  }
  
  public void renderError(ServiceContext context)
    throws IOException
  { new ErrorTag(tag).render(context);
  }
    
  /**
   * <p>Create a new Action target for the Form post
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  protected Action createAction(EventContext context)
  {
    int[] path=context.getState().getPath();
    
    String pathString=ArrayUtil.format(path,".",null);
    
    return new Action
      (pathString
      ,path
      )
    {
      
      @Override
      @SuppressWarnings("unchecked") // Blind cast
      public void invoke(ServiceContext context)
      { 
        if (debug)
        {
          log.fine
            ("Form: Generic action invoked: "
            +ArrayUtil.format(getTargetPath(),"/",null)
            );
        }

        FormState<T> formState
          =(FormState<T>) context.getState();
        
        if (context.getPost()!=null)
        {
          // Only gather if there was a POST (as opposed to a GET,
          //   which would delete data or throw NPEs if we make the
          //   controls gather.
          relayMessage(context,GATHER_MESSAGE,null);
        
          if (!formState.isErrorState())
          { 
            // Don't run commands if any vars have errors
            relayMessage(context,COMMAND_MESSAGE,null);
          }
          
          if (onPostChannel!=null && !formState.isErrorState())
          { onPostChannel.get().execute();
          }
          
        }
        
      }
    };
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    FormState<T> state=((FormState<T>) context.getState());
    if (state.isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
  }
  
  @Override
  public FormState<T> createState()
  { return new FormState<T>(this);
  }
  
  @Override
  public Focus<?> bindExports()
    throws BindException
  {
    if (onPost!=null)
    { onPostChannel=getFocus().bind(onPost);
    }
    return null;
    
  }
  
  public class FormState<X>
    extends ControlGroupState<X>
  {
    public FormState(Form<X> form)
    { super(form);
    }

  }
}
