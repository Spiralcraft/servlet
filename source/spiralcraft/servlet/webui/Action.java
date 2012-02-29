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

import spiralcraft.util.Sequence;

/**
 * <p>A potential user input directed at a specific webui Component instance
 *   or Component State instance
 * </p>
 * 
 * @author mike
 *
 */
public abstract class Action
{
  
  private final String name;
  private final Sequence<Integer> targetPath;
  protected boolean responsive=true;

  public Action(String name,Sequence<Integer> targetPath)
  { 
    this.name=name;
    this.targetPath=targetPath;
  }
  
  /**
   * <p>Indicates that an Action is set up for a user to respond to a specific
   *   output element, and should be cleared in-between requests.
   * </p>
   * 
   * <p>An action that is responsive is effectively a single-use action,
   *   one that would normally be generated from a specific rendering.
   * </p>
   * 
   * <P>An action that is not responsive would typically be used to handle
   *   explicit invocation of functionality, eg. via the URL, and should not 
   *   depend on the state of the component as it may linger from the
   *   last request. 
   * </P>
   *   
   * 
   * @return Whether the Action will be cleared after handling user input.
   */
  public boolean isResponsive()
  { return responsive;
  }
  
  /**
   * 
   * @return The path through the component tree to the component which
   *   is the target of this action
   */
  public Sequence<Integer> getTargetPath()
  { return targetPath;
  }
  
  public String getName()
  { return name;
  }
  
  public abstract void invoke(ServiceContext context);

}
