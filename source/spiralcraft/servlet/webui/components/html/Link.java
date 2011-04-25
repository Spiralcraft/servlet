//
//Copyright (c) 1998,2011 Michael Toth
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

import spiralcraft.command.Command;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.Level;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.components.LinkAcceptor;

import spiralcraft.util.ArrayUtil;

import java.io.IOException;

/**
 * <P>A Link, bound to a Command. The "x" (binding target) property contains an
 *   expression that resolves an instance of a Command to execute.
 * </P>
 * 
 * <P>&lt;INPUT type="<i>image</i>" alt="<i>sometext</i>" 
 *  src="<i>imageURI</i>"&gt;
 * </P>
 *  
 * @author mike
 *
 */
public class Link
  extends Component
{
  
  private Expression<Command<?,?,?>> commandExpression;
  private Channel<Command<?,?,?>> commandChannel;
  private Tag tag=new Tag();
  private ErrorTag errorTag=new ErrorTag(tag);
  private String[] queueActions;
  private LinkAcceptor<?> linkAcceptor;
  private String fragment;
  private Binding<String> fragmentX;
  
  /**
   * An Expression which evaluates to the command that will be invoked when
   *   the link is clicked.
   *   
   * @param expression
   */
  public void setX(Expression<Command<?,?,?>> expression)
  { commandExpression=expression;
  }
  
  /**
   * <p>An Expression which evaluates to the fragment portion of the URL that
   *   will be generated. 
   * </p>
   * 
   * <p>If the static fragment property is also specified, it will be used 
   *   as a prefix to this value.
   * </p>
   *   
   * @param fragmentX
   */
  public void setFragmentX(Binding<String> fragmentX)
  { this.fragmentX=fragmentX;
  }
  
  /**
   * <p>A static fragment portion of the URL that will be generated.
   * </p>
   * 
   * <p>If the fragmentX property is also specified, it will be prefixed with
   *   this value.
   * </p>
   *   
   * @param fragmentX
   */
  public void setFragment(String fragment)
  { this.fragment=fragment;
  }
  
  public class Tag extends AbstractTag
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
          .registerAction(createAction(context));
      
      String fragmentVal=null;
      if (fragmentX!=null)
      { fragmentVal=fragmentX.get();
      }
      
      if (fragmentVal==null)
      { fragmentVal=fragment;
      }
      else if (fragment!=null)
      { fragmentVal=fragment+fragmentVal;
      }
      
      if (fragmentVal!=null)
      { actionURI=actionURI+"#"+fragmentVal;
      }
      
      renderAttribute(context.getOutput(),"href",actionURI);
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
    
  }
  
  @Override
  public Focus<?> bind(Focus<?> focus)
    throws ContextualException
  { 
    if (commandExpression!=null)
    { commandChannel=focus.bind(commandExpression);
    }

    linkAcceptor=LangUtil.findInstance(LinkAcceptor.class,focus);

    tag.bind(focus);
    errorTag.bind(focus);
    
    if (fragmentX!=null)
    { fragmentX.bind(focus);
    }
    return super.bind(focus);    
  }
  
  public void setQueueActions(String[] actionNames)
  { this.queueActions=actionNames;
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { tag.render(context);
    
  }
  

  
  protected Action createAction(EventContext context)
  {
    int[] path=context.getState().getPath();
    
    String pathString=ArrayUtil.format(path,".",null);

    return new Action(pathString,path)
    {
      Command<?,?,?> command=commandChannel!=null?commandChannel.get():null;
      
      @Override
      public void invoke(ServiceContext context)
      { 
        if (command!=null)
        {
          command.execute();
          if (command.getException()!=null)
          { 
            log.log
              (Level.WARNING
              ,Link.this.getErrorContext()+": Error running link command"
              ,command.getException()
              );
          }
        }
        
        // XXX Note, this should not be global, need to come up with
        //   contextual trigger mechanism
        if (queueActions!=null)
        { 
          for (String actionName:queueActions)
          { 
            if (debug)
            { log.fine("Queuing action "+actionName);
            }
            ServiceContext.get().queueAction(actionName);
          }
        }
        
        if (linkAcceptor!=null)
        { linkAcceptor.linkActioned();
        }
      }
    };
  }
}
