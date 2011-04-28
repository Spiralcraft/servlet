//
//Copyright (c) 2011 Michael Toth
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

import spiralcraft.servlet.webui.Component;
import spiralcraft.app.Dispatcher;

/**
 * References a client side script. Placeholder for addition of capabilites
 *   to manage compressed or in-lined scripts.
 * 
 * @author mike
 *
 */
public class Script
  extends Component
{

  private String type="text/javascript";
  private String src;
  
  private AbstractTag tag
    =new AbstractTag()
  {
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "script";
    }


    @Override
    protected void renderAttributes(Dispatcher context,Appendable out)
      throws IOException
    {   
      renderAttribute(out,"type",Script.this.type);
      renderAttribute(out,"src",src);
      
      super.renderAttributes(context,out);
      
    }    


    @Override
    protected boolean hasContent()
    { return true;
    }

  };
  
  
  { addHandler(tag);
  }

}
