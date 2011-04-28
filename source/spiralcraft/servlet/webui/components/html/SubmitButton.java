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

import spiralcraft.app.Dispatcher;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.AbstractCommandControl;
import spiralcraft.servlet.webui.components.CommandState;
import spiralcraft.net.http.VariableMap;

/**
 * <P>A standard Submit button, bound to a Command. The "x" (binding target)
 *   property contains an expression that resolves an instance of a Command to 
 *   execute.
 * </P>
 * 
 * <P>&lt;INPUT type="<i>submit</i>"&gt;
 * </P>
 *  
 * @author mike
 *
 */
public class SubmitButton<Tcontext,Tresult>
  extends AbstractCommandControl<Tcontext,Tresult>
{

  private String name;
  private String label;
  
  private Tag tag=new Tag();
  
  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "input";
    }

    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      renderAttribute(out,"type","submit");
      renderAttribute(out,"name",getState(context).getVariableName());
      
      // Yes, we are renaming it
      renderAttribute(out,"value",label);
      super.renderAttributes(context,out);
    }

    @Override
    protected boolean hasContent()
    { return false;
    }
  }
    
  private ErrorTag errorTag=new ErrorTag();
  
  
  { 
    addHandler(errorTag);
    addHandler(tag);
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setLabel(String label)
  { this.label=label;
  }


  @Override
  public String getVariableName()
  { return name;
  }

  
  @Override
  public void gather(ServiceContext context)
  {
    CommandState<Tcontext,Tresult> state=getState(context);
    VariableMap post=context.getPost();
    boolean gotPost=false;
    if (post!=null)
    { gotPost=post.getOne(state.getVariableName())!=null;
    }

    if (gotPost)
    { executeCommand(context);
    }
    
    
    if (debug)
    { 
      log.fine
        ("SubmitButton: readPost- "+state.getVariableName()+"="
            +context.getPost().getOne(state.getVariableName())
        );
    }
  }
  
  @Override
  public void scatter(ServiceContext context)
  { 
  }


}

