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

/**
 * A potential user input directed at a specific webui Component associated
 *   with 
 * 
 * @author mike
 *
 */
public abstract class Action
{
  
  private final int[] targetPath;
  protected boolean clearable=true;

  public Action(int[] targetPath)
  { this.targetPath=targetPath;
  }
  
  /**
   * <P>Indicates that an Action should be cleared after handling user input.
   * </P>
   * 
   * <P>An action that is always clearable is effectively a single-use action,
   *   one that would normally be generated from a specific rendering.
   * </P>
   * 
   * <P>An action that is not clearable would typically be used to handle
   *   explicit invocation of functionality via the URL.
   * </P>
   *   
   * 
   * @return Whether the Action will be cleared after handling user input.
   */
  public boolean isClearable()
  { return clearable;
  }
  
  /**
   * 
   * @return The path through the component tree to the component which
   *   is the target of this action
   */
  public int[] getTargetPath()
  { return targetPath;
  }
  
  public abstract void invoke(ServiceContext context);

}
