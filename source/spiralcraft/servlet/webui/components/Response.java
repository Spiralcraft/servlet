//
//Copyright (c) 2009,2009 Michael Toth
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
package spiralcraft.servlet.webui.components;



import java.util.ArrayList;
import java.util.List;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;

import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.app.Dispatcher;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.Message;
import spiralcraft.app.kit.AbstractMessageHandler;

import spiralcraft.common.ContextualException;

/**
 * <p>Provide a means to interact with the HTTP response
 * </p>
 * 
 * @author mike
 *
 */
public class Response
  extends Component
{

  private List<Assignment<?>> assignments
    =new ArrayList<Assignment<?>>();

  
  private List<Setter<?>> setters
    =new ArrayList<Setter<?>>();

  {
    this.addHandler
      (new AbstractMessageHandler()
    {

      @Override
      protected void doHandler
        (Dispatcher context, Message message,MessageHandlerChain next)
      {
        for (Setter<?> setter:setters)
        { setter.set();
        }
        next.handleMessage(context,message);
      }

    }
    );
  }
  
  /**
   * <p>Specify a bound expression for the response contentType.
   * </p>
   * 
   * @param contentTypeX
   */
  public void setContentTypeX(Expression<String> contentTypeX)
  { 
    assignments.add
      (new Assignment<String>
        (Expression.<String>create("contentType")
        ,contentTypeX
        )
      );
  }
  
  @Override
  protected Focus<?> bindStandard(Focus<?> focus)
    throws ContextualException
  { 

    focus=
      (focus.chain
        (focus.bind
           (Expression.<ServiceContext>create
             ("[:class:/spiralcraft/servlet/webui/ServiceContext]")
           )
        )
      );
    
    for (Assignment<?> assignment: assignments)
    { setters.add(assignment.bind(focus));
    }
    return super.bindStandard(focus);
  }  
  



}



