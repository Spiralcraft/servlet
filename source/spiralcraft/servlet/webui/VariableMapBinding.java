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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import spiralcraft.log.Level;

import spiralcraft.lang.Channel;
import spiralcraft.log.ClassLog;
import spiralcraft.net.http.VariableMap;
import spiralcraft.text.ParseException;
import spiralcraft.text.translator.Translator;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.string.StringConverter;

/**
 * <p>Associates a request or form VariableMap with a target Channel.
 * </p>
 * 
 * @author mike
 *
 */
@SuppressWarnings("unchecked") // Casts related to StringConverter and arrays
public class VariableMapBinding<Tvar>
{
  private static final ClassLog log
    =ClassLog.getInstance(VariableMapBinding.class);

  private final Channel<Tvar> target;
  private final String name;
  private StringConverter converter;
  private boolean array;
  private Class<Tvar> clazz;
  private boolean passNull;
  private boolean debug;
  private Translator translator;
  
  
  

  public VariableMapBinding(Channel<Tvar> target,String name)
  {
    this.target=target;
    this.name=name;
    clazz=target.getContentType();
    if (clazz.isArray())
    { 
      array=true;
      converter
        =StringConverter.getInstance(clazz.getComponentType());
    }
    else
    {
      converter
        =StringConverter.getInstance(clazz);
    }
  }

  /**
   * <p>Specify a Translator which sits between the VariableMap and the
   *   StringConverter and normalizes data read from the VariableMap and
   *   published from the target. 
   * </p>
   * 
   * @param translator
   */
  public void setTranslator(Translator translator)
  { this.translator=translator;
  }
  
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  /**
   * <p>Whether the binding will set the target value to null of the bound
   *   request variable is not present
   * </p>
   * @param passNull
   */
  
  public void setPassNull(boolean passNull)
  { this.passNull = passNull;
  }
  
  /**
   * <p>Specifies the StringConverter which will provide the bidirectional
   *   conversion from a String to the native type of the binding target
   * </p>
   * 
   * @param converter
   */
  public void setConverter(StringConverter converter)
  { this.converter=converter;
  }
  
  private Object translateValueIn(String val)
  {
    if (val==null)
    { return null;
    }
    
    String tval=val;
    if (translator!=null)
    { 
      try
      { tval=translator.translateIn(val);
      }
      catch (ParseException x)
      {
        // XXX This method should throw something explicit
        log.log(Level.WARNING,"Error translating "+val,x);
        throw new RuntimeException(x);
      }
    }
    
    if (converter!=null)
    { return converter.fromString(tval);
    }
    else
    { return tval;
    }
  }
  
  
  private String translateValueOut(Object val)
  {
    if (val==null)
    { return null;
    }
    
    String sval;
    if (converter!=null)
    { sval=converter.toString(val);
    }
    else
    { sval=(String) val;
    }
    
    String tval=sval;
    if (translator!=null)
    { 
      try
      { tval=translator.translateOut(sval);
      }
      catch (ParseException x)
      {
        // XXX This method should throw something explicit
        log.log(Level.WARNING,"Error translating "+sval,x);
        throw new RuntimeException(x);
      }
        
    }
    return tval;
  }
  
  /**
   * <p>Translate the value from the target into a List<String> for publishing
   *   to the URI query string.
   * </p>
   * 
   * <p>Since URL-encoded variables can have multiple values, a List<String>
   *   is always used, even if the variable has a single value.
   * </p>
   * 
   * <p>The String values returned from this method must be further encoded
   *   for inclusion in the URL.
   * </p>
   * 
   * @return A List of String-encoded values associated with the variable.
   */
  public List<String> translate()
  {
    if (array)
    {
      Tvar array=target.get();
      if (debug)
      { 
        log.fine
          ("Translating : "
          +ArrayUtil.format(array, "," ,"\"")
          );
      }
        
      if (array==null)
      { return null;
      }
      else
      {
        int len = Array.getLength(array);
        List<String> ret=new ArrayList<String>(len);
        for (int i=0;i<len;i++)
        { 
          Object val=Array.get(array,i);
          String sval=translateValueOut(val);
          if (sval!=null)
          { ret.add(sval);
          }
        }
        return ret;
      }
    }
    else
    {
      Tvar val=target.get();
      if (debug)
      { 
        log.fine
          ("Translating : "
          +val
          );
      }
      String sval=translateValueOut(val);
      if (sval==null)
      { return null;
      }
      else
      {
        List<String> ret=new ArrayList<String>(1);
        ret.add(sval);
        return ret;
      }
    }
  }
  
  /**
   * <p>Read data from the map and write it to the target channel.
   * </p>
   * 
   * <p>The VariableMap represents data read from an HTTP request in the form
   *   of a set of variable names mapped to one or more values.
   * </p>
   * 
   * <p>
   * @param map
   */
  public void read(VariableMap map)
  {
    List<String> vals=map!=null?map.get(name):null;
    if (debug)
    { log.fine("Reading "+vals+" for "+name);
    }
    
    if (vals!=null && vals.size()>0)
    { 

      if (array)
      {
        Object array=Array.newInstance(clazz.getComponentType());
        array=ArrayUtil.expandBy(array, vals.size());
        int i=0;
        for (String val : vals)
        { 
          Array.set(array, i++, translateValueIn(val));
        }
         
        if (debug)
        { log.fine("Setting target to "+array);
        }
        target.set((Tvar) array);
          
      }
      else
      { 
        Object value=translateValueIn(vals.get(0));
        if (debug)
        { log.fine("Setting target to "+value);
        }
        target.set((Tvar)value);
      }
        
    }
    else if (passNull)
    { 
      if (debug)
      { log.fine("Setting target to null for "+name);
      }
      target.set(null);
    }
  }
  

  
  
}
