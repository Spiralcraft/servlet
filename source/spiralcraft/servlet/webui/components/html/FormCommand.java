//
//Copyright (c) 1998,2008 Michael Toth
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

import java.io.IOException;

import spiralcraft.textgen.EventContext;

import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.components.AcceptorCommand;

import spiralcraft.lang.BindException;

/**
 * <p>Triggers Command execution during the GATHER phase of an Acceptor,
 *   which occurs when the Acceptor is actioned. 
 * </p>
 * 
 * <p>Can be triggered either by the value of the "name" property appearing
 *   in a "command" variable in the POST or URI query
 *   String, and/or when a boolean condition specified in the "whenX" property
 *   evaluates to "true".
 * </p>
 * 
 * <p>If neither "name" or "whenX" are specified, the Command will be triggered
 *   every time the form receives a POST.
 * </p>
 *
 * <p>If both "name" and "whenX" are specified, the Command will be triggered
 *   only when both conditions are satisfied.
 * </p>
 *  
 * 
 * @author mike
 *
 */
public class FormCommand
  extends AcceptorCommand
{

  
  private Tag tag=new Tag();
  
  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return null;
    }

    @Override
    protected boolean hasContent()
    { return false;
    }
  }
    
  private ErrorTag errorTag=new ErrorTag(tag);
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  
  @Override
  protected void bindSelf()
    throws BindException
  {
    super.bindSelf();
    tag.bind(getFocus());
    errorTag.bind(getFocus());
  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    if (((ControlState<?>) context.getState()).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
    super.render(context);
  }
  
}

