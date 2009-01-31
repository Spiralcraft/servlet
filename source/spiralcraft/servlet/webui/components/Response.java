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
package spiralcraft.servlet.webui.components;



import java.util.List;



import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;

import spiralcraft.servlet.webui.Component;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.compiler.TglUnit;


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




  @Override
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    Focus<?> parentFocus=getParent().getFocus();

    super.bind(childUnits);
  }  
  


}



