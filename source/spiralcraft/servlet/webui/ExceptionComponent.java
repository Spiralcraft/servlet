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

import java.io.Flushable;
import java.io.IOException;


import spiralcraft.app.Dispatcher;
import spiralcraft.common.ContextualException;
import spiralcraft.textgen.OutputContext;
import spiralcraft.textgen.kit.RenderHandler;

public class ExceptionComponent
    extends RootComponent
{
  private ExceptionInfo info;

  public ExceptionComponent(ExceptionInfo info)
  { this.info=info;
  }

  @Override
  protected void addHandlers() 
    throws ContextualException
  { 
    addHandler
      (new RenderHandler()
        { 
          @Override
          protected void render(Dispatcher context)
            throws IOException
          { 
            ServiceContext serviceContext=(ServiceContext) context;
            serviceContext.getResponse().setStatus(500);   
            
            Appendable out=OutputContext.get();
            out.append("<html><body><pre>");
            ((Flushable) out).flush();
            Throwable parent=info.exception;
            while (parent!=null)
            {
              out.append(parent.toString()).append("\r\n");
              for (StackTraceElement element: info.exception.getStackTrace())
              { out.append("    ").append(element.toString()).append("\r\n");
              }
              parent=parent.getCause();
              if (parent!=null)
              { out.append("Caused by: ");
              }
            }

            out.append("</pre></body></html>");
            
          }
        
        } 
      );
    super.addHandlers();
  }
  
  
}
