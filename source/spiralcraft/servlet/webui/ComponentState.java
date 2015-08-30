//
//Copyright (c) 2012 Michael Toth
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


import spiralcraft.textgen.ElementState;
import spiralcraft.util.Sequence;
import spiralcraft.util.string.StringPool;

public class ComponentState
  extends ElementState
{

  private String id;
    
  public ComponentState(Component component)
  { super(component.getChildCount());
  }
  

  
  public String getId()
  {
    if (id==null)
    { id=StringPool.INSTANCE.get(pathToId(getPath()));
    }
    return id;
  }
  
  private String pathToId(Sequence<Integer> path)
  {
    StringBuilder builder=new StringBuilder();
    builder.append("sid");
    for (Integer integer:path)
    {
      builder.append("-");
      builder.append(integer);
    }
    return builder.toString();
  }
  


}
