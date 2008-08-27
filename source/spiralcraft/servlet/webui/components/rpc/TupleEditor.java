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
package spiralcraft.servlet.webui.components.rpc;

import java.io.IOException;

import spiralcraft.data.session.Buffer;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.textgen.EventContext;

/**
 * <p>A TupleEditor geared towards RPC based editing
 * </p>
 * 
 * @author mike
 *
 */
public class TupleEditor
    extends spiralcraft.servlet.webui.components.TupleEditor
{

  @Override
  public void render(EventContext context)
    throws IOException
  { 
    renderChildren(context);
  }

  @Override
  protected Focus<Buffer> bindExports()
    throws BindException
  {

    return super.bindExports();
  }
  

}
