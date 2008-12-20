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
package spiralcraft.servlet.webui;

import spiralcraft.lang.BindException;
import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.Element;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.InitializeMessage;
import spiralcraft.textgen.PrepareMessage;

import spiralcraft.textgen.Message;
import spiralcraft.textgen.compiler.TglUnit;


import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>A Component is a unit of web user interface composition. 
 * </p>
 * 
 * <p>Components form a tree under a root component. The root component
 *   is normally addressable via a URI path. 
 * </p>
 * 
 * <p>The output mechanism for a Component relies on the Text Generation system,
 *   defined by the spiralcraft.textgen package. Every Component is also a
 *   spiralcraft.textgen.Element (a text generation unit). 
 * </p>
 * 
 * <p>
 *   Integration with the textgen package allows for general purpose text
 *     generation Elements to be combined with web aware Components in order
 *     to create web specific interactive applications that incorporate
 *     standard functionality.
 * </p>
 * 
 * <p>The input mechanism for a Component consists of externally addressable
 *   Actions delivered to components from the containing servlet.
 * </p>
 * 
 * @author mike
 *
 */
public abstract class Component
  extends Element
{
  private Component parentComponent;
  
//  private Expression<?> beforeRequest;
//  private Expression<?> afterRequest;
//  
//  private Expression<?> beforePrepare;
//  private Expression<?> afterPrepare;
//  
//  private Expression<?> beforeRender;
//  private Expression<?> afterRender;
  
  public Component getParentComponent()
  { return parentComponent;
  }
  

  @Override
  public void setParent(Element parentElement)
    throws MarkupException
  {
    super.setParent(parentElement);
    if (parentElement!=null)
    { 
      this.parentComponent
        =parentElement.<Component>findElement(Component.class);
    }

  }

  @Override
  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    )
  {
//    // Copious
    if (debug)
    { log.fine(this.toString()+" message "+message);
    }
    
    if (message.getType()==RequestMessage.TYPE)
    { handleRequest((ServiceContext) context);
    }
    else if (message.getType()==ActionMessage.TYPE)
    {
      if (((ActionMessage) message).getAction().getTargetPath()
           ==context.getState().getPath()
         )
      { 
        handleAction
          ((ServiceContext) context
          ,((ActionMessage) message).getAction()
          );
      }
    }
    else if (message.getType()==CommandMessage.TYPE)
    { handleCommand((ServiceContext) context);
    }
    else if (message.getType()==PrepareMessage.TYPE)
    { 
      handlePrepare((ServiceContext) context);
    }
    else if (message.getType()==InitializeMessage.TYPE)
    { handleInitialize((ServiceContext) context);
    }
    
    
    super.message(context,message,path);

    if (message.getType()==CommandMessage.TYPE)
    { handleCommand((ServiceContext) context);
    }

  }
  
  public void destroy()
  {
  }
  
  /**
   * <p>Called as soon as a request comes in, before actions
   *   are triggered.
   * </p>
   * 
   * <p>This method is visited in pre-order
   * </p>
   * 
   * @param context
   */
  protected void handleRequest(ServiceContext context)
  {
  }

  protected void handleCommand(ServiceContext context)
  {
  }
  
  protected void handleAction(ServiceContext context,Action action)
  {
    //System.err.println
    //  ("Component: "+getClass().getName()+": Default handleAction(): "
    //    +action
    //    );
    action.invoke(context);
  }
  
  /**
   * Override to initialize before rendering. Subclasses should always call
   *   this superclass method to process URL commands
   * 
   * @param context
   */
  protected void handlePrepare(ServiceContext context)
  { 
    
  }

  /**
   * Override to initialize for a new session
   * 
   * @param context
   */
  protected void handleInitialize(ServiceContext context)
  { }


  
  @Override
  public void render(EventContext context)
    throws IOException
  { renderChildren(context);
  }
  
  @Override
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    bindChildren(childUnits);
  }
}
