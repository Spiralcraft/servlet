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


import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.Type;
import spiralcraft.data.session.DataSession;

import spiralcraft.data.session.DataSessionFocus;

import spiralcraft.lang.BindException;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLogger;
import spiralcraft.net.http.VariableMap;

import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.ElementState;
import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.Message;
import spiralcraft.textgen.compiler.TglUnit;

public class DataSessionComponent
  extends Component
{
  private static final ClassLogger log
    =ClassLogger.getInstance(DataSessionComponent.class);

  private DataSessionFocus dataSessionFocus;
  private Type<DataComposite> type;
  private RequestBinding<?>[] requestBindings;
  
  private ThreadLocalChannel<DataSession> dataSessionChannel;
  private CompoundFocus<DataComposite> dataFocus;

  @SuppressWarnings("unchecked")
  @Override
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    
    Focus<?> parentFocus=getParent().getFocus();
    log.fine("DataSession.bind() "+parentFocus);
    dataSessionChannel
      =new ThreadLocalChannel<DataSession>
        (BeanReflector.<DataSession>getInstance
           (DataSession.class)
        );
    dataSessionFocus
      =new DataSessionFocus(parentFocus,dataSessionChannel,type);
    dataFocus=new CompoundFocus
      (dataSessionFocus,dataSessionFocus.findFocus(type.getURI()).getSubject());
    dataFocus.bindFocus("spiralcraft.data",dataSessionFocus);
    bindRequestAssignments();
    bindChildren(childUnits);
  }
  
  @Override
  public Focus<?> getFocus()
  { return dataFocus;
  }
 
  @SuppressWarnings("unchecked")
  public void setTypeURI(URI typeURI)
  { 
    try
    { this.type=(Type<DataComposite>) Type.resolve(typeURI);
    }
    catch (DataException x)
    { throw new IllegalArgumentException(x);
    }
     
  }

  @SuppressWarnings("unchecked")
  // Blind cast
  @Override
  public void render(EventContext context) throws IOException
  {

    try
    {
      dataSessionChannel.push
        ( ((DataSessionState) context.getState()).get() );
      super.render(context);
    } 
    finally
    {
      dataSessionChannel.pop();
    }
  }
    
  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    ) 
  {

    try
    {
      dataSessionChannel.push
        ( ((DataSessionState) context.getState()).get() );
      super.message(context,message,path);
    } 
    finally
    {
      dataSessionChannel.pop();
    }
  }

  @Override
  public ElementState createState()
  { return new DataSessionState
      (dataSessionFocus.newDataSession(),getChildCount());
  }
  
  @Override
  public void handlePrepare(ServiceContext context)
  { 
    applyRequestBindings(context);
  }
  
  @SuppressWarnings("unchecked")
  private void bindRequestAssignments()
    throws BindException
  {
    if (requestBindings==null)
    { return;
    }

    for (RequestBinding binding:requestBindings)
    { 
      binding.bind(getFocus());
      if (debug)
      { binding.setDebug(true);
      }
    }
  }
  
  private void applyRequestBindings(ServiceContext context)
  {
    if (requestBindings!=null)
    {
      VariableMap query=context.getQuery();
      for (RequestBinding<?> binding: requestBindings)
      { 
        binding.getBinding().read(query);
        binding.publish(context);
      }
    }
  }
  
  public void setRequestBindings(RequestBinding<?>[] bindings)
  { requestBindings=bindings;
  }
  
}

class DataSessionState
  extends ElementState
{

  private DataSession session;
  
  public DataSessionState(DataSession session,int childCount)
  { 
    super(childCount);
    this.session=session;
  }
  
  public DataSession get()
  { return session;
  }
  
}

