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
import spiralcraft.lang.Contextual;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;

import spiralcraft.lang.parser.AssignmentNode;
import spiralcraft.lang.util.ExpressionStringConverter;

import spiralcraft.log.ClassLog;
import spiralcraft.net.http.VariableMap;
import spiralcraft.net.http.VariableMapBinding;

import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.text.translator.Translator;

import spiralcraft.util.string.StringConverter;

/**
 * Provides access to the request Query string as part of a UI
 *   element
 *
 * @author mike
 *
 */
public class RequestBinding<Tval>
  implements Contextual
{
  private static final ClassLog log
    =ClassLog.getInstance(RequestBinding.class);
  
  private String name;
  private Expression<Tval> target;
  private boolean passNull;
  private boolean publish;
  private boolean debug;
  private Translator translator;
  private StringConverter<Tval> converter;
  private boolean assignment;
  private boolean trim;
  
  private VariableMapBinding<Tval> binding;

  public RequestBinding(String name)
  { 
    this.name=name;
    this.target=Expression.create(name);
  }
  
  public RequestBinding()
  {
  }
  
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

  /**
   * Trim incoming and outgoing strings
   * 
   * @param trim
   */
  public void setTrim(boolean trim)
  { this.trim=trim;
  }
  
  public Expression<Tval> getTarget()
  { return target;
  }

  public void setTarget(Expression<Tval> target)
  { 
    this.target = target;
    if (this.target.getRootNode() instanceof AssignmentNode<?,?>)
    { assignment=true;
    }
  }
  
  public void setTranslator(Translator translator)
  { this.translator=translator;
  }
  
  /**
   * <p>Specifies the StringConverter which will provide the bidirectional
   *   conversion from a String to the native type of the binding target
   * </p>
   * 
   * @param converter
   */
  public void setConverter(StringConverter<Tval> converter)
  { this.converter=converter;
  }
  
  public VariableMapBinding<Tval> getBinding()
  { return binding;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Focus<?> bind(Focus<?> focus)
    throws BindException
  { 
    if (assignment)
    { 
      if (converter!=null)
      { 
        throw new BindException
          ("In binding for "+name+": Cannot set "
          +" both a target assignment and a converter"
          );
      }
      
      Channel<Tval> targetChannel
        =focus.bind
          (Expression.<Tval>create
            ( ((AssignmentNode<Tval,Tval>)target.getRootNode()).getTarget())
          );
      if (debug)
      { log.fine("Bound target "+targetChannel);
      }
      
      binding=new VariableMapBinding<Tval>
        (targetChannel
        ,name
        ,new ExpressionStringConverter<Tval>
          (focus
          ,Expression.<Tval>create
            ( ((AssignmentNode<Tval,Tval>) target.getRootNode()).getSource()
            )
          ,targetChannel.getReflector()
          )
        );
    }
    else
    { 
      Channel<Tval> targetChannel=focus.bind(target);
      if (debug)
      { log.fine("Bound target "+targetChannel);
      }
      
      binding=new VariableMapBinding<Tval>(targetChannel,name,converter);
    }
    binding.setPassNull(passNull);
    binding.setDebug(debug);
    binding.setTrim(trim);
    if (translator!=null)
    { binding.setTranslator(translator);
    }
    return focus;
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
      
  public void read(VariableMap map)
  { binding.read(map);
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
