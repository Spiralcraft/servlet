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


import java.net.URI;

import spiralcraft.common.ContextualException;
import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.Type;
import spiralcraft.data.session.DataSession;

import spiralcraft.data.session.DataSessionFocus;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;

import spiralcraft.net.http.VariableMap;

import spiralcraft.servlet.webui.Component;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.ElementState;

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;

public class DataSessionComponent
  extends Component
{

  private DataSessionFocus dataSessionFocus;
  private Type<DataComposite> type;
  private RequestBinding<?>[] requestBindings;
  
  private ThreadLocalChannel<DataSession> dataSessionChannel;
  private Focus<DataComposite> dataFocus;
  private Assignment<?>[] defaultAssignments;
  private Assignment<?>[] assignments;
  private Assignment<?>[] requestAssignments;  
 
  private Setter<?>[] defaultSetters;  
  private Setter<?>[] requestSetters;
  private Setter<?>[] setters;
  
  private Expression<Type<DataComposite>> typeX;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Focus<?> bind(Focus<?> focus)
    throws  ContextualException
  { 
    if (debug)
    { log.fine("DataSession.bind() "+focus);
    }
    if (type==null && typeX!=null)
    { type=focus.bind(typeX).get();
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
      =new DataSessionFocus(focus,dataSessionChannel,type);
  
    if (debug)
    { dataSessionFocus.setDebug(true);
    }
    
    if (type!=null)
    {
      // Pull the data from the dataSession focus
      dataFocus=new SimpleFocus
        (dataSessionFocus,dataSessionFocus.findFocus(type.getURI()).getSubject());
    
      dataFocus.addFacet(dataSessionFocus);
    }
    
    focus=dataFocus!=null?dataFocus:dataSessionFocus;

    focus.addFacet(getAssembly().getFocus());
    
    requestSetters=Assignment.bindArray(requestAssignments,focus);
    defaultSetters=Assignment.bindArray(defaultAssignments,focus);
    setters=Assignment.bindArray(assignments,focus);
    bindRequestBindings(focus);
    return super.bind(focus);
  }
  
//  @Override
//  public Focus<?> getFocus()
//  { return dataFocus!=null?dataFocus:dataSessionFocus;
//  }
// 

  /**
   * <p>Default Assignments get executed for null assignment target values 
   *   when a new state frame is being computed, (either at the beginning
   *   of a non-responsive request, or on "prepare", after all input has been 
   *   processed)
   * </p>
   *   
   * @param assignments
   */
  public void setDefaultAssignments(Assignment<?>[] assignments)
  { defaultAssignments=assignments;
  }

  /**
   * <p>Request assignments always get executed at the beginning of each 
   *   request in order to prepare the session for new input.
   * </p>
   * @param assignments
   */
  public void setRequestAssignments(Assignment<?>[] assignments)
  { this.requestAssignments=assignments;
  }
  
  /**
   * <p>Assignments get executed whenever a new state frame is being
   *   computed (either at the beginning of a non-responsive request, 
   *   or on "prepare", after all input has been processed)
   * </p>
   * @param assignments
   */
  public void setAssignments(Assignment<?>[] assignments)
  { this.assignments=assignments;
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
  public void message
    (Dispatcher context
    ,Message message
    ) 
  {


    DataSessionState state=getState(context);
    dataSessionChannel.push(state.get());
    try
    {
      if (!state.isInitialized())
      { 
        dataSessionFocus.initializeDataSession();
        state.setInitialized(true);
      }
    
      
      super.message(context,message);
      if (debug)
      { 
        log.fine
          ("Data: "+getState(context).get().getData());
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
  
  protected DataSessionState getState(Dispatcher context)
  { return (DataSessionState) context.getState();
  }
  
  @Override
  public void handleRequest(ServiceContext context)
  { 
		DataSessionState state=getState(context);
		
		// Always run to prep for input
		applyRequestAssignments(context);
		
    // Leave the session object alone until handlePrepare()
    //   for a responsive request
    if (state.isNewFrame())
    {
      applyRequestBindings(context);
      applyAssignments();
      state.setRequestBindingsApplied(true);
    }
    else
    {
      if (debug)
      { log.debug("Skipping request bindings- continuation of previous request");
      }
    }
  } 

  @Override
  public void handlePrepare(ServiceContext context)
  { 
    DataSessionState state=getState(context);
    if (state.isNewFrame())
    { 
    	if (!state.getRequestBindingsApplied())
    	{
    	  // Only do this here if we didn't do it in handleRequest()
    	  applyRequestBindings(context);
    	}
    	else
    	{ 
    	  if (debug)
    	  { 
    	    log.debug
    	      ("Not applying request bindings on frame change because they"
    	      +" have been applied at the Request stage"
    	      );
    	  }
    	}
    }
    state.setRequestBindingsApplied(false);
    
    applyAssignments();
    publishRequestBindings(context);
  } 

  private void bindRequestBindings(Focus<?> focus)
    throws BindException
  {
    if (requestBindings==null)
    { return;
    }

    for (RequestBinding<?> binding:requestBindings)
    { 
      if (debug)
      { binding.setDebug(true);
      }
      binding.bind(focus);
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
      { binding.read(query);
      }
    }
  }
  
  private void applyRequestAssignments(ServiceContext context)
  {
    if (requestSetters!=null)
    {
      if (debug)
      { log.fine("Applying request assignments");
      }

      Setter.applyArray(requestSetters);
    }
  }  

  private void applyAssignments()
  {
    if (setters!=null)
    { 
      if (debug)
      { log.fine(toString()+": applying assignments");
      }
      Setter.applyArray(setters);
    }

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
  


  
}

class DataSessionState
  extends ElementState
{

  private DataSession session;
  private boolean initialized;
  private boolean requestBindingsApplied;
  
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
  
  /**
   * <p>For backwards compatibility- should be removed as soon as we figure out
   *   what requires this. 
   * </p>
   * 
   * <p>Why should we avoid applying request bindings on PREPARE, if we
   *   already applied them on REQUEST.
   * </p>
   * 
   * 
   * @return
   */
  public boolean getRequestBindingsApplied()
  { return requestBindingsApplied;
  }
  
  public void setRequestBindingsApplied(boolean val)
  { requestBindingsApplied=val;
  }
  
}

