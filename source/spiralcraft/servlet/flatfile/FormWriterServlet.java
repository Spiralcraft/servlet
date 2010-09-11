//
// Copyright (c) 1998,2005 Michael Toth
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
package spiralcraft.servlet.flatfile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;
import java.util.ArrayList;
//import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.data.DataException;
import spiralcraft.data.Field;
import spiralcraft.data.Space;
import spiralcraft.data.Tuple;
import spiralcraft.data.Type;

import spiralcraft.data.access.Updater;
import spiralcraft.data.core.FieldImpl;
import spiralcraft.data.flatfile.Writer;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.spi.EditableArrayTuple;

import spiralcraft.lang.reflect.BeanFocus;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;

import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.util.DictionaryBinding;

import spiralcraft.net.http.VariableMap;
import spiralcraft.net.http.VariableMapBinding;

import spiralcraft.servlet.HttpServlet;

import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
//import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.UnresolvableURIException;
import spiralcraft.vfs.file.FileResource;

import java.io.BufferedOutputStream;

/**
 * <p>Writes simple Form data to a flat file (.csv) according to the scheme
 *   of a spiralcraft.data.Type.
 * </p>
 * 
 * <p>This class is primarily intended to work with manually coded 
 *   HTML forms. 
 * </p>
 * 
 * @author mike
 *
 */
public class FormWriterServlet
    extends HttpServlet
{

  private FileResource resource;
  private URI resourceURI;
  
  private ThreadLocalChannel<Tuple> channel;
  private Writer writer;
  private Updater<Tuple> updater;
  private Type<?> type;
  private Focus<?> focus;
  private DictionaryBinding<?>[] bindings;
  private VariableMapBinding<?>[] fieldBindings;
  private OutputStream out;
  
  public void setResourceURI(URI uri)
  { this.resourceURI=uri;
  }
  
  public void setType(Type<?> type)
  { this.type=type;
  }
  
  public void setBindings(DictionaryBinding<?>[] bindings)
  { this.bindings=bindings;
  }
  

  @SuppressWarnings({"unchecked","rawtypes"})
  @Override
  public void init(ServletConfig config)
    throws ServletException
  { 
    super.init(config);
    try
    {
      Resource resource=null;
      if (!resourceURI.isAbsolute())
      { resource=this.getResource(resourceURI.getPath());
      }
      else
      { resource=Resolver.getInstance().resolve(resourceURI);
      }
      
      if (resource instanceof FileResource)
      { this.resource=(FileResource) resource;
      }
      else
      { 
        throw new ServletException
          ("Resource is not a local file: "+resource);
      }
    }
    catch (UnresolvableURIException x)
    { throwServletException("Unresolveable URI "+resourceURI,x);
    }

    try
    {
      Focus<Space> context
        =new BeanFocus(new Space());

      channel=new ThreadLocalChannel<Tuple>
        (DataReflector.<Tuple>getInstance(type));
      
      focus=new SimpleFocus<Tuple>(context,channel);
      
      updater=new Updater(context);
      updater.dataInitialize(type.getFieldSet());
      
      boolean exists=resource.exists();
      
      out=new BufferedOutputStream
        (new FileOutputStream(resource.getFile().getAbsolutePath(),true)
        );
      
      writer=new Writer(out);
      writer.setAutoFlush(true);
      writer.setWriteHeader(!exists);
      writer.dataInitialize(type.getFieldSet());
      
      if (bindings==null)
      {
        // Create automatic bindings for the fieldSet
        ArrayList<VariableMapBinding<?>> maps=
            new ArrayList<VariableMapBinding<?>>();
        for (Field field: type.getFieldSet().fieldIterable())
        {
          if (field.getClass().equals(FieldImpl.class))
          {
            maps.add
              (new VariableMapBinding
                (focus.bind(Expression.create(field.getName()))
                ,field.getName()
                ,null
                )
              );

          }
        }
        fieldBindings=maps.toArray(new VariableMapBinding<?>[maps.size()]);
      }
      else
      {
        // Use the configured mappings
        ArrayList<VariableMapBinding<?>> maps=
            new ArrayList<VariableMapBinding<?>>();
        
        for (DictionaryBinding binding: bindings)
        {
          maps.add
            (new VariableMapBinding
              (focus.bind(binding.getTarget())
              ,binding.getName()
              ,null
              )
            );
        }
        fieldBindings=maps.toArray(new VariableMapBinding<?>[maps.size()]);
      }
    }
    catch (DataException x)
    { throwServletException("Error initializing updater for "+type,x);
    }
    catch (BindException x)
    { throwServletException("Error reflecting type "+type,x);
    }
    catch (IOException x)
    { 
      throwServletException
        ("Error opening output file for writing: "+resource.getURI(),x);
    }
  }
  
  /**
   * <p>Provides a default implementation of service(request,response) which
   *   delegates handling to different methods according to the HTTP request
   *   method.
   * </p>
   */
  @SuppressWarnings("unchecked")
  @Override
  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException,IOException
  {
    channel.push(new EditableArrayTuple(type));
    try
    {
    
      if (request.getContentType().equals("application/x-www-form-urlencoded"))
      { 
        // Use request.getParameterMap() because we might be getting called
        //   via a dispatch, where the content has been read already
        VariableMap map
          =new VariableMap(request.getParameterMap());
//        String postString
//          =StreamUtil.readAsciiString
//            (request.getInputStream(),request.getContentLength());

        for (VariableMapBinding<?> binding : fieldBindings)
        { binding.read(map);
        }
      }
      updater.dataAvailable(channel.get());
      writer.dataAvailable(channel.get());
    }
    catch (DataException x)
    { throw new ServletException("Error writing data",x);
    }
    finally
    { channel.pop();
    }
    
  }

}
