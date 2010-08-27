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

import java.io.IOException;
import java.io.PrintStream;

import spiralcraft.textgen.EventContext;

public class ExceptionComponent
    extends RootComponent
{
  private Throwable exception;
  public ExceptionComponent(Throwable exception)
  { this.exception=exception;
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    context.getWriter().write("<html><body><pre>");
    context.getWriter().flush();
    exception.printStackTrace
      (new PrintStream(((ServiceContext) context).getResponse().getOutputStream())
      );
    context.getWriter().write("</pre></body></html>");
  }
}
