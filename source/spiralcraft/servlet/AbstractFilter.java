//
// Copyright (c) 2009 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.servlet;

import java.util.Enumeration;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import spiralcraft.lang.util.Configurator;
import spiralcraft.log.ClassLog;


/**
 * <p>Base class for Filters, with useful utility methods to simplify
 *  implementations.
 * </p>
 */
public abstract class AbstractFilter
  implements Filter
{

  
  protected final ClassLog log=
    ClassLog.getInstance(getClass());

  protected FilterConfig config;
  protected Set<String> recognizedParameters;
  protected boolean autoConfigure=true;
  private Configurator<?> configurator;
  protected ContextAdapter contextAdapter;

  @Override
  @SuppressWarnings("unchecked")
  public void init(FilterConfig config)
    throws ServletException
  { 
    this.config=config;
    this.contextAdapter=new ContextAdapter(config.getServletContext());    
    
    Enumeration<String> names=config.getInitParameterNames();
    if (names!=null)
    {
      while (names.hasMoreElements())
      {
        String name=names.nextElement();
        if (recognizedParameters!=null 
            && !recognizedParameters.contains(name)
           )
        {
          throw new ServletException
            ("Unrecognized init parameter '"+name+"'. Recognized parameters are "
            +" "+recognizedParameters.toString()
            );
        }
        setInitParameter(name,config.getInitParameter(name));
      
      }
    }
  }
  
  /**
   * <p>Override to handle initialization parameters. The default
   *   implementation invokes the automatic configuration mechanism
   *   if autoConfigure=true (the default case) which maps the name and
   *   value to the bean properties if this object.
   * </p>
   * 
   * <p>Used in conjunction with the protected 'recognizedParameters' set,
   *   parameters not listed in the set will not reach this method.
   * </p>
   * 
   * 
   * @param name
   * @param value
   * @throws ServletException
   */
  protected void setInitParameter(String name,String value)
    throws ServletException
  { 
    if (autoConfigure)
    {
      if (configurator==null)
      { configurator=Configurator.forBean(this);
      }
      configurator.set(name,value);
    }
  }
}
