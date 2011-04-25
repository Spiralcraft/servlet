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

import java.util.List;

import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.textgen.compiler.TglUnit;

/**
 * <p>An area for editing a particular data element, which includes a set
 *   of input controls, a label, and message areas. 
 * </p>
 * 
 * @author mike
 *
 * @param <T>
 */
public class FormField<T>
  extends ControlGroup<T>
{
  
  // Needs a div tag here
  
  @Override
  protected List<TglUnit> expandChildren(Focus<?> focus,List<TglUnit> children)
  { 
    // Generate children from the focus type
    
    return children;
  }
}
