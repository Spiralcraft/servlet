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
import java.io.PrintStream;

import spiralcraft.app.Dispatcher;
import spiralcraft.textgen.OutputContext;
import spiralcraft.textgen.kit.RenderHandler;

public class ExceptionComponent
    extends RootComponent
{
  private Throwable exception;
  
  { addHandler
      (new RenderHandler()
        { 
          @Override
          protected void render(Dispatcher context)
            throws IOException
          { 
            Appendable out=OutputContext.get();
            out.append("<html><body><pre>");
            ((Flushable) out).flush();
            exception.printStackTrace
              (new PrintStream(((ServiceContext) context).getResponse().getOutputStream())
              );
            out.append("</pre></body></html>");
          }
        
        } 
      );
  }
  
  public ExceptionComponent(Throwable exception)
  { this.exception=exception;
  }
  
  
  
}
