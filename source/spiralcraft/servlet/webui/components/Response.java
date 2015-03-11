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



import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.RenderMessage;
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

  private Binding<String> contentTypeX;
  private Binding<byte[]> binaryContentX;

  @Override
  protected void addHandlers()
    throws ContextualException
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
        
        HttpServletResponse response
          =((ServiceContext) context).getResponse();
        if (contentTypeX!=null)
        { response.setContentType(contentTypeX.get());
        }        
        
        if (message.getType()==RenderMessage.TYPE)
        {  

          if (binaryContentX!=null)
          { 
            byte[] bytes=binaryContentX.get();
            if (bytes!=null)
            {
              response.setContentLength(bytes.length);
              try
              { response.getOutputStream().write(bytes);
              }
              catch (SocketException x)
              { 
                log.info
                  ("Connection reset during binary transfer from "+getDeclarationInfo());
              }
              catch (IOException x)
              { 
                throw new RuntimeException
                  ("Error writing response ("+getDeclarationInfo()+")",x);
              }
            }
            else
            { response.setContentLength(0);
            }
          }
        }
        next.handleMessage(context,message);
      }

    }
    );
    super.addHandlers();
  }
  
  /**
   * <p>Specify a bound expression for the response contentType.
   * </p>
   * 
   * @param contentTypeX
   */
  public void setContentTypeX(Binding<String> contentTypeX)
  { this.contentTypeX=contentTypeX;
  }
  
  /**
   * <p>Specify an optional binding that will send binary
   *   content to the client
   * </p>
   * 
   * @param binaryContentX
   */
  public void setBinaryContentX(Binding<byte[]> binaryContentX)
  { this.binaryContentX=binaryContentX;
  }
  
    
  @Override
  protected Focus<?> bindStandard(Focus<?> focus)
    throws ContextualException
  { 

    Focus<ServiceContext> scFocus=
      (focus.chain
        (LangUtil.assertChannel(ServiceContext.class,focus)
        )
      );
    
    for (Assignment<?> assignment: assignments)
    { setters.add(assignment.bind(scFocus));
    }
    if (contentTypeX!=null)
    { contentTypeX.bind(focus);
    }
    if (binaryContentX!=null)
    { binaryContentX.bind(focus);
    }
    return super.bindStandard(focus);
  }  
  



}



