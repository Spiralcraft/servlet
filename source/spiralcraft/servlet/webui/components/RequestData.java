//
//Copyright (c) 1998,2009 Michael Toth
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.Signature;
import spiralcraft.lang.parser.StructNode;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.ExpressionFocusElement;
import spiralcraft.textgen.ValueState;


/**
 * <p>Assigns data from the Request to the target object specified by 
 *   the "x" expression. The target will be published into the context
 *   exported by this component.
 * </p> 
 * 
 * @author mike
 *
 * @param <T>
 */
public class RequestData<T>
  extends ExpressionFocusElement<T>
{
  private RequestBinding<?>[] requestBindings;
  private String[] excludedNames;
  private boolean autoMap;
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected Focus<?> bindExports(Focus<?> focusChain) throws BindException
  {     
    if (requestBindings==null || autoMap)
    {
      Reflector<?> targetReflector=focusChain.getSubject().getReflector();
      if (autoMap || targetReflector instanceof StructNode.StructReflector)
      { 
        List<Signature> sigs
          =targetReflector.getSignatures(focusChain.getSubject());
        
        ArrayList<RequestBinding> autoBindings
          =new ArrayList<RequestBinding>();
        
        HashSet<String> excludedNames=new HashSet<String>();
        if (this.excludedNames!=null)
        { 
          for (String name:this.excludedNames)
          { excludedNames.add(name);
          }
        }
        
        if (requestBindings!=null)
        { 
          for (RequestBinding requestBinding: requestBindings)
          { 
            autoBindings.add(requestBinding);
            excludedNames.add(requestBinding.getName());
          }
        }
        
        for (Signature sig:sigs)
        {
          if (sig.getParameters()==null 
              && !excludedNames.contains(sig.getName())
              )
          { 
            RequestBinding requestBinding=new RequestBinding();
            requestBinding.setName(sig.getName());
            requestBinding.setTarget(Expression.create("."+sig.getName()));
            autoBindings.add(requestBinding);
          }
        }
        requestBindings
          =autoBindings.toArray(new RequestBinding[autoBindings.size()]);
      }
    }
    
    if (requestBindings!=null)
    { 
      for (RequestBinding<?> requestBinding:requestBindings)
      { requestBinding.bind(focusChain);
      }
    }
    return focusChain;
    
  }
  
  /**
   * Automatically map request parameters which correspond to the properties
   *   of the object referenced by the target expression. 
   *   
   * @param autoMap
   */
  public void setAutoMap(boolean autoMap)
  { this.autoMap=autoMap;
  }
  
  /**
   * Exclude the specified property names from automatic mapping
   * 
   * @param excludedNames
   */
  public void setExcludedNames(String[] excludedNames)
  { this.excludedNames=excludedNames;
  }
  
  /**
   * An optional set of mappings to use for specific request fields
   * 
   * @param requestBindings
   */
  public void setRequestBindings(RequestBinding<?>[] requestBindings)
  { this.requestBindings=requestBindings;
  }
  
  @Override
  protected T computeExportValue(ValueState<T> state)
  {
    T value=super.computeExportValue(state);
    
    
    if (requestBindings!=null)
    {
      ServiceContext context=ServiceContext.get();
      for (RequestBinding<?> requestBinding:requestBindings)
      { requestBinding.read(context.getQuery());
      }
    }
    // Apply bindings here
    return value;
    
  }
  
}

