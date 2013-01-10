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
import spiralcraft.lang.Binding;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.Signature;
import spiralcraft.lang.parser.StructNode;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.ExpressionFocusElement;
import spiralcraft.textgen.ValueState;
import spiralcraft.util.ArrayUtil;


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
  public static enum Source
  { 
    QUERY,
    POST;
  }
  
  private RequestBinding<?>[] requestBindings;
  private ThreadLocalChannel<T> prevalue;
  private String[] includedNames;
  private String[] excludedNames;
  private String[] publishNames;
  private boolean autoMap;
  private Source source=Source.QUERY;
  private Binding<?> onRequest;
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected Focus<?> bindExports(Focus<?> focusChain) throws BindException
  {     
    prevalue
      =new ThreadLocalChannel<T>
        ((Reflector<T>) focusChain.getSubject().getReflector());
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
        
        HashSet<String> includedNames=null;
        if (this.includedNames!=null && this.includedNames.length>0)
        { 
          includedNames=new HashSet<String>();
          for (String name:this.includedNames)
          { includedNames.add(name);
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
              && (includedNames==null
                  || includedNames.contains(sig.getName())
                 )
              && !excludedNames.contains(sig.getName())
              && !sig.getName().startsWith("@")
              )
          { 
            RequestBinding requestBinding=new RequestBinding();
            requestBinding.setName(sig.getName());
            requestBinding.setTarget(Expression.create("."+sig.getName()));
            if (debug)
            { requestBinding.setDebug(true);
            }
            if (publishNames!=null 
                && ArrayUtil.contains(publishNames,sig.getName())
                )
            { requestBinding.setPublish(true);
            }
            autoBindings.add(requestBinding);
          }
        }
        requestBindings
          =autoBindings.toArray(new RequestBinding[autoBindings.size()]);
      }
    }
    
    Focus<?> prefocus=focusChain.chain(prevalue);
    if (requestBindings!=null)
    { 
      for (RequestBinding<?> requestBinding:requestBindings)
      { 
        requestBinding.bind(prefocus);
        if (debug)
        { log.fine("Bound "+requestBinding+" for "+requestBinding.getName());
        }
      }
    }
    if (onRequest!=null)
    { onRequest.bind(prefocus);
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
  public void setExcludeNames(String[] excludedNames)
  { this.excludedNames=excludedNames;
  }
  
  /**
   * Include the specified property names in automatic mapping and turn
   *   automatic mapping on if the included names are not empty
   *   
   * @param includedNames
   */
  public void setIncludeNames(String[] includedNames)
  { 
    if (includedNames!=null && includedNames.length>0)
    { setAutoMap(true);
    }
    this.includedNames=includedNames;
  }
  
  /**
   * Republish the specified property names in back-links to this page
   * 
   * @param excludedNames
   */
  public void setPublishNames(String[] publishNames)
  { this.publishNames=publishNames;
  }
  
  public void setSource(Source source)
  { this.source=source;
  }
  
  /**
   * An optional set of mappings to use for specific request fields
   * 
   * @param requestBindings
   */
  public void setRequestBindings(RequestBinding<?>[] requestBindings)
  { this.requestBindings=requestBindings;
  }
  
  /**
   * An expression to evaluate once the request data has been read
   * 
   * @param onRequest
   */
  public void setOnRequest(Binding<?> onRequest)
  { this.onRequest=onRequest;
  }
  
  @Override
  protected T computeExportValue(ValueState<T> state)
  {
    T value=super.computeExportValue(state);
    
    prevalue.push(value);
    try
    {
      if (requestBindings!=null)
      {
        ServiceContext context=ServiceContext.get();
        for (RequestBinding<?> requestBinding:requestBindings)
        { 
          requestBinding.read
            (source==Source.QUERY
              ?context.getQuery()
              :context.getPost()
             );
          requestBinding.publish(context);
        }
      }
      if (onRequest!=null)
      { onRequest.get();
      }
      return value;
    }
    finally
    { prevalue.pop();
    }
    
  }
  
  
}

