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
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;

import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.VariableMapBinding;

/**
 * Provides access to the request Query string as part of a UI
 *   element
 *
 * @author mike
 *
 */
public class RequestBinding<Tval>
{
  private String name;
  private Expression<Tval> target;
  private boolean passNull;
  private boolean publish;
  private boolean debug;
  
  private VariableMapBinding<Tval> binding;

  public String getName()
  { return name;
  }
  
  public boolean isPassNull()
  { return passNull;
  }

  public void setPassNull(boolean passNull)
  { this.passNull = passNull;
  }

  public void setName(String name)
  { this.name = name;
  }

  public Expression<Tval> getTarget()
  { return target;
  }

  public void setTarget(Expression<Tval> target)
  { this.target = target;
  }
  
  public VariableMapBinding<Tval> getBinding()
  { return binding;
  }
  
  public void bind(Focus<?> focus)
    throws BindException
  { 
    Channel<Tval> targetChannel=focus.bind(target);
    binding=new VariableMapBinding<Tval>(targetChannel,name);
    binding.setPassNull(passNull);
    binding.setDebug(debug);
  
  }
  
  public void setPublish(boolean publish)
  { this.publish = publish;
  }
  
  public void setDebug(boolean debug)
  { 
    this.debug=debug;
    if (binding!=null)
    { binding.setDebug(debug);
    }
  }
      
  public void publish(ServiceContext context)
  {
    if (publish)
    { 
      List<String> values=binding.translate();
      if (values!=null)
      { context.setActionParameter(name, values);
      }
    }
  }
 
}
