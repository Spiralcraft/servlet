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

import spiralcraft.lang.Channel;
import spiralcraft.log.ClassLogger;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.webui.components.Editor;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.StringConverter;

/**
 * Associates a request or form variable map with a target Channel.
 *
 * @author mike
 *
 */
public class VariableMapBinding<Tvar>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(VariableMapBinding.class);

  private final Channel<Tvar> target;
  private final String name;
  private StringConverter converter;
  private boolean array;
  private Class<Tvar> clazz;
  private boolean passNull;
  private boolean debug;
  

  @SuppressWarnings("unchecked")
  public VariableMapBinding(Channel<Tvar> target,String name)
  {
    this.target=target;
    this.name=name;
    clazz=target.getContentType();
    if (clazz.isArray())
    { 
      array=true;
      converter
        =(StringConverter<Tvar>)
          StringConverter.getInstance(clazz.getComponentType());
    }
    else
    {
      converter
        =(StringConverter<Tvar>)
          StringConverter.getInstance(clazz);
    }
  }

  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  public void setPassNull(boolean passNull)
  { this.passNull = passNull;
  }
  
  /**
   * Translate the value from the target into a List<String> for publishing
   * 
   * @return
   */
  public List<String> translate()
  {
    if (converter!=null)
    {
      if (array)
      {
        Tvar array=target.get();
        if (debug)
        { 
          log.fine
            ("Translating with "+converter+": "
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
            if (val!=null)
            { ret.add(converter.toString(val));
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
            ("Translating with "+converter+": "
            +val
            );
        }
        if (val==null)
        { return null;
        }
        else
        {
          List<String> ret=new ArrayList<String>(1);
          ret.add(converter.toString(val));
          return ret;
        }
      }
    }
    else
    {
      if (array)
      {
        Tvar array=target.get();
        if (debug)
        { 
          log.fine
            ("Translating with "+converter+": "
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
            String val=(String) Array.get(array,i);
            if (val!=null)
            { ret.add(val);
            }
          }
          return ret;
        }
      }
      else
      {
        String val=(String) target.get();
        if (debug)
        { 
          log.fine
            ("Translating raw "
            +val
            );
        }
        if (val==null)
        { return null;
        }
        else
        {
          List<String> ret=new ArrayList<String>(1);
          ret.add(val);
          return ret;
        }
      }
      
    }
  }
  
  @SuppressWarnings("unchecked")
  public void read(VariableMap map)
  {
    List<String> vals=map!=null?map.get(name):null;
    if (debug)
    { log.fine("Reading "+vals);
    }
    
    if (vals!=null && vals.size()>0)
    { 
      if (converter!=null)
      {
        if (array)
        {
          Object array=Array.newInstance(clazz.getComponentType());
          array=ArrayUtil.expandBy(array, vals.size());
          int i=0;
          for (String val : vals)
          { 
            Array.set(array, i++, converter.fromString(val));
          }
          
          if (debug)
          { log.fine("Setting target to "+vals);
          }
          target.set((Tvar) array);
          
        }
        else
        { 
          Object value=converter.fromString(vals.get(0));
          if (debug)
          { log.fine("Setting target to "+value);
          }
          target.set((Tvar)value);
        }
        
      }
      else
      {
        if (array)
        {
          String[] array=new String[vals.size()];
          vals.toArray(array);
          if (debug)
          { log.fine("Setting target to "+array);
          }
          target.set((Tvar) array);
        }
        else
        {
          if (debug)
          { log.fine("Setting target to "+vals.get(0));
          }
          target.set((Tvar) vals.get(0));
        }
        
      }
    }
    else if (passNull)
    { 
      if (debug)
      { log.fine("Setting target to null");
      }
      target.set(null);
    }
  }
  

  
  
}
