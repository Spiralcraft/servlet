//
//Copyright (c) 1998,2012 Michael Toth
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
package spiralcraft.servlet.webui.kit;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import spiralcraft.app.State;
import spiralcraft.app.StateFrame;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.servlet.webui.Action;
import spiralcraft.util.ListMap;
import spiralcraft.util.Sequence;
import spiralcraft.util.URIUtil;

/**
 * Tracks the current state of interaction with a "port" ie. a component that
 *   can have a conversation with a client
 * 
 * @author mike
 *
 */
public class PortSession
{
  
  private static final ClassLog log
    =ClassLog.getInstance(PortSession.class);
  private static final Level logLevel
    =ClassLog.getInitialDebugLevel(PortSession.class, null);

  public static enum RequestSyncStatus
  {
    RESPONSIVE
    ,INITIATED
    ,OUTOFSYNC
  };
  
  private final ListMap<String,Action> actionMap
    =new ListMap<String,Action>();

  private final ListMap<String,Sequence<Integer>> portMap
    =new ListMap<String,Sequence<Integer>>();
    
  private final VariableMap parameterMap
    =new VariableMap();
  
  private volatile StateFrame currentFrame;  
  
  private final PortSession parentSession;
    
  private String localURI;
  
  private State state;
  private Sequence<Integer> port;
  
  public PortSession()
  { parentSession=null;
  }
  
  public PortSession(PortSession parentSession)
  { this.parentSession=parentSession;
  }

  
  public State getState()
  { return state;
  }
  
  public void setState(State state)
  { this.state=state;
  }
  
  public void setPort(Sequence<Integer> port)
  { this.port=port;
  }
  
  /**
   * 
   * @param action The Action to register
   * @param preferredName If not null, this name is used as the "action" query 
   *    parameter, otherwise a random string is generated
   * @return A URI that invokes the specified action
   */
  public String registerAction(Action action)
  {     

    if (logLevel.isDebug())
    { log.fine("Registered "+action);
    }
      
    actionMap.add(action.getName(),action);
    String encodedParameters=parameterMap.generateEncodedForm();
    return localURI
      +"?action="+action.getName()
      +"&lrs="+currentFrame.getId()
      +(port!=null?"&port="+port.format("."):"")
      +(encodedParameters!=null?"&"+encodedParameters:"")
      ;
    
  }  
  
    /**
   * 
   * @param action The Action to register
   * @param preferredName If not null, this name is used as the "action" query 
   *    parameter, otherwise a random string is generated
   * @return A URI that invokes the specified action
   */
  public String registerPort(String id,Sequence<Integer> path)
  {  
    if (parentSession!=null)
    { return parentSession.registerPort(id,path);
    }

    if (logLevel.isDebug())
    { log.fine("Registered port "+id+" -> "+path);
    }
      
    portMap.add(id,path);
    String encodedParameters=parameterMap.generateEncodedForm();
    return localURI
      +"?port="+id
      +"&lrs="+currentFrame.getId()
      +(encodedParameters!=null?"&"+encodedParameters:"")
      ;
    
  } 
  
  public Sequence<Integer> getPort(String id)
  { return portMap.getFirst(id);
  } 
  
  /**
   * The URI that will prefix the self-referencing URLs generated by 
   *   registerAction(). Usually set to the HttpServletRequest.requestURI.
   * 
   * @param localURI
   */
  public void setLocalURI(String localURI)
  { this.localURI=localURI;
  }
  
  /**
   * The URI that will prefix the self-referencing URLs generated by 
   *   registerAction(). Usually set to the HttpServletRequest.requestURI.
   */
  public String getLocalURI()
  { return localURI;
  }
  
  public synchronized void clearActions()
  { 
    LinkedList<Action> actions=new LinkedList<Action>();
    actionMap.toValueList(actions);

    // Remove clearable Actions only
    for (Action action:actions)
    {
      if (action.isResponsive())
      { actionMap.removeValue(action.getName(),action);
      }
    }

    parameterMap.clear();
    portMap.clear();
  }
  
  public void clearParameters()
  { parameterMap.clear();
  }
    
  public StateFrame currentFrame()
  { return currentFrame;
  }

  public List<Action> getActions(String name)
  { return actionMap.get(name);
  }

  /**
   * <p>Indicate whether the "lrs" or "last request state" provided in the 
   *   request query synchronizes with this ResourceSession
   * </p>
   *   
   * @param query
   */
  public RequestSyncStatus getRequestSyncStatus(String requestedFrame)
  {

    
    if (currentFrame!=null && currentFrame.getId().equals(requestedFrame))
    { return RequestSyncStatus.RESPONSIVE;
    }
    else
    {       
      if (requestedFrame==null || requestedFrame.isEmpty())
      { return RequestSyncStatus.INITIATED;
      }
      else
      { return RequestSyncStatus.OUTOFSYNC;
      }
    }
  }  
  
  public StateFrame nextFrame()
  { 
    currentFrame=new StateFrame();
    return currentFrame;
  }
  
  public void setFrame(StateFrame frame)
  { currentFrame=frame;
  }
  
  public String getAsyncURL()
  {
    String encodedParameters=parameterMap.generateEncodedForm();
    return localURI
      +"?lrs="+currentFrame.getId()
      +(port!=null?"&port="+port.format("."):"")
      +(encodedParameters!=null?"&"+encodedParameters:"")
      ;
  }
  
    /**
   * @return A link back to the current resource that contains published
   *  parameters and maintains the conversation state
   */
  public String getAbsoluteBackLink(HttpServletRequest request)
  {
    String encodedParameters=parameterMap.generateEncodedForm();
    URI requestURI=URI.create(request.getRequestURL().toString());
    
    URI backLink
      =URIUtil.replaceRawQuery
        (requestURI
        ,"lrs="+currentFrame.getId()
          +(port!=null?"&port="+port.format("."):"")
          +(encodedParameters!=null?"&"+encodedParameters:"")
        );
    return backLink.toString();
    
  }
  
   
  public VariableMap getActionParameters()
  { return parameterMap;
  }
  
  public String getEncodedParameters()
  { return parameterMap.generateEncodedForm();
  }

  
  public void setActionParameter(String name,String[] values)
  { setActionParameter(name,Arrays.asList(values));
  }
  
  public void setActionParameter(String name, List<String> values)
  { 
    if (values!=null)
    { parameterMap.put(name,values);
    }
    else
    { parameterMap.remove(name);
    }
  }
}
