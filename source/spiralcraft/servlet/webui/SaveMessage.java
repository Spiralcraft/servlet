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
 * A message that directs a tree of Editors or other buffer management
 *   components to commit any buffered edits to the application model.
 * 
 * @author mike
 */
public class SaveMessage
  extends UIMessage
{
  public static final Type TYPE=new Type();
  public static final SaveMessage INSTANCE = new SaveMessage();

  
  public SaveMessage()
  { 
    super(TYPE);
    transactional=true;
    multicast=true;
  }

}