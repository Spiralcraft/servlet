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

import spiralcraft.app.Dispatcher;

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
import spiralcraft.task.Eval;

import spiralcraft.util.Sequence;

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
  private ErrorTag errorTag=new ErrorTag();
  private String[] queueActions;
  private LinkAcceptor<?> linkAcceptor;
  private String fragment;
  private Binding<String> fragmentX;
  private Expression<Void> onAction;
  
  
  
  { 
    addHandler(errorTag);
    addHandler(tag);
  }
  
  /**
   * An Expression to be evaluated when the link is clicked
   * 
   * @param onAction
   */
  public void setOnAction(Expression<Void> onAction)
  { this.onAction=onAction;
  }

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
    protected String getTagName(Dispatcher dispatcher)
    { return "a";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
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
      
      renderAttribute(out,"href",actionURI);
      super.renderAttributes(context,out);
    }

    @Override
    protected boolean hasContent()
    { return getChildCount()>0;
    }
    
    
  }
  
  @Override
  public Focus<?> bindStandard(Focus<?> focus)
    throws ContextualException
  { 
    if (onAction!=null)
    { 
      Eval<Void,Void> eval=new Eval<Void,Void>(onAction);
      eval.bind(focus);
      commandChannel=LangUtil.constantChannel(eval)
        .resolve(focus,"command",new Expression[0]);
      
    }
    else if (commandExpression!=null)
    { commandChannel=focus.bind(commandExpression);
    }

    linkAcceptor=LangUtil.findInstance(LinkAcceptor.class,focus);

    if (fragmentX!=null)
    { fragmentX.bind(focus);
    }
    return super.bindStandard(focus);    
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
  
  
  protected Action createAction(Dispatcher context)
  {
    Sequence<Integer> path=context.getState().getPath();
    
    String pathString=path.format(".");

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
