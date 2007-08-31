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


import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.Element;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;


import java.io.IOException;
import java.util.LinkedList;

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
public abstract class Component<T>
  extends Element<T>
{

  private Component<?> parentComponent;
    


  public Component<?> getParentComponent()
  { return parentComponent;
  }
  

  @SuppressWarnings("unchecked") // Can't parameterize reflective operation
  @Override
  public void setParent(Element<?> parentElement)
    throws MarkupException
  {
    super.setParent(parentElement);
    if (parentElement!=null)
    { 
      this.parentComponent
        =parentElement.<Component>findElement(Component.class);
    }
  }

  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    )
  {
    if (message.getType()==ActionMessage.TYPE)
    {
      if (((ActionMessage) message).getAction().getTargetPath()==getPath())
      { 
        handleAction
          ((ServiceContext) context
          ,((ActionMessage) message).getAction()
          );
      }
    }
    super.message(context,message,path);
  }
  
  public void destroy()
  {
  }

  protected void handleAction(ServiceContext context,Action action)
  {
    System.err.println
      (getClass().getName()+": Default handleAction(): "+action);
    action.invoke(context);
  }

  @Override
  public void render(EventContext context)
    throws IOException
  { renderChildren(context);
  }
  
}
