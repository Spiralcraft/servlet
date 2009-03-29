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

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.CompoundFocus;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;

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

  private DataSessionFocus dataSessionFocus;
  private Type<DataComposite> type;
  private RequestBinding<?>[] requestBindings;
  
  private ThreadLocalChannel<DataSession> dataSessionChannel;
  private CompoundFocus<DataComposite> dataFocus;
  private Assignment<?>[] defaultAssignments;
  private Setter<?>[] defaultSetters;
  private Expression<Type<DataComposite>> typeX;

  @SuppressWarnings("unchecked")
  @Override
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    
    Focus<?> parentFocus=getParent().getFocus();
    if (debug)
    { log.fine("DataSession.bind() "+parentFocus);
    }
    if (type==null && typeX!=null)
    { type=parentFocus.bind(typeX).get();
    }
//    if (type==null)
//    { throw new BindException("No type specified "+getErrorContext());
//    }
    
    dataSessionChannel
      =new ThreadLocalChannel<DataSession>
        (BeanReflector.<DataSession>getInstance
           (DataSession.class)
        ,true
        );
    dataSessionFocus
      =new DataSessionFocus(parentFocus,dataSessionChannel,type);
  
    if (type!=null)
    {
      // Pull the data from the dataSession focus
      dataFocus=new CompoundFocus
        (dataSessionFocus,dataSessionFocus.findFocus(type.getURI()).getSubject());
    
      dataFocus.bindFocus("spiralcraft.data",dataSessionFocus);
    }
    defaultSetters=bindAssignments(defaultAssignments);
    bindRequestAssignments();
    bindChildren(childUnits);
  }
  
  @Override
  public Focus<?> getFocus()
  { return dataFocus!=null?dataFocus:dataSessionFocus;
  }
 
  /**
   * <p>Default Assignments get executed when the target value is null
   *   in the "prepare" stage of request processing
   * </p>
   *   
   * @param assignments
   */
  public void setDefaultAssignments(Assignment<?>[] assignments)
  { defaultAssignments=assignments;
  }
  
  public void setTypeX(Expression<Type<DataComposite>> typeX)
  { this.typeX=typeX;
  }
  
  public void setTypeURI(URI typeURI)
  { 
    try
    { this.type= Type.<DataComposite>resolve(typeURI);
    }
    catch (DataException x)
    { throw new IllegalArgumentException(x);
    }
     
  }

  @Override
  public void render(EventContext context) throws IOException
  {

    try
    {
      DataSessionState state=(DataSessionState) context.getState();
      
      dataSessionChannel.push(state.get());
      if (!state.isInitialized())
      { 
        dataSessionFocus.initializeDataSession();
        state.setInitialized(true);
      }
      
      super.render(context);
    } 
    finally
    {
      dataSessionChannel.pop();
    }
  }

  
  @Override
  public void message
    (EventContext context
    ,Message message
    ,LinkedList<Integer> path
    ) 
  {

    try
    {
      DataSessionState state=(DataSessionState) context.getState();
      dataSessionChannel.push(state.get());
      if (!state.isInitialized())
      { 
        dataSessionFocus.initializeDataSession();
        state.setInitialized(true);
      }
    
      
      super.message(context,message,path);
      if (debug)
      { 
        log.fine
          (((DataSessionState) context.getState()).get().getData().toString());
      }
    } 
    finally
    {
      dataSessionChannel.pop();
    }
  }

  
  @Override
  public ElementState createState()
  { 
    return new DataSessionState
      (dataSessionFocus.newDataSession(),getChildCount());
  }
  
  @Override
  public void handleRequest(ServiceContext context)
  { 

    // Leave the session object alone until handlePrepare()
    //   for a responsive request
    if (!context.isResponsive())
    {
      applyRequestBindings(context);
      applyDefaults();
    }
  } 

  @Override
  public void handlePrepare(ServiceContext context)
  { 
    if (context.isResponsive())
    { 
      // Only do this here if we didn't do it in handleRequest()
      applyRequestBindings(context);
    }
    
    applyDefaults();
    publishRequestBindings(context);
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
      if (debug)
      { binding.setDebug(true);
      }
      binding.bind(getFocus());
    }
  }
  
  private void applyRequestBindings(ServiceContext context)
  {
    if (requestBindings!=null)
    {
      if (debug)
      { log.fine("Applying request bindings");
      }
      VariableMap query=context.getQuery();
      for (RequestBinding<?> binding: requestBindings)
      { binding.getBinding().read(query);
      }
    }
  }

  private void applyDefaults()
  {
    if (defaultSetters!=null)
    { 
      if (debug)
      { log.fine(toString()+": applying default values");
      }
      Setter.applyArrayIfNull(defaultSetters);
    }
  }
  
  private void publishRequestBindings(ServiceContext context)
  {
    if (requestBindings!=null)
    {
      if (debug)
      { log.fine("Publishing request bindings");
      }
      for (RequestBinding<?> binding: requestBindings)
      { binding.publish(context);
      }
    }
  }
  
  public void setRequestBindings(RequestBinding<?>[] bindings)
  { requestBindings=bindings;
  }
  
  private Setter<?>[] bindAssignments(Assignment<?>[] assignments)
    throws BindException
  {
    if (assignments!=null)
    {
      Setter<?>[] setters=new Setter<?>[assignments.length];
      int i=0;
      for (Assignment<?> assignment: assignments)
      { setters[i++]=assignment.bind(getFocus());
      }
      return setters;
    }
    return null;
  }

  
}

class DataSessionState
  extends ElementState
{

  private DataSession session;
  private boolean initialized;
  
  public DataSessionState(DataSession session,int childCount)
  { 
    super(childCount);
    this.session=session;
    
  }
  
  public boolean isInitialized()
  { return initialized;
  }
  
  public void setInitialized(boolean initialized)
  { this.initialized=initialized;
  }
  
  public DataSession get()
  { return session;
  }
  
}

