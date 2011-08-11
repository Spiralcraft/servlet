package spiralcraft.servlet.webui;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.app.StateFrame;
import spiralcraft.util.ListMap;
import spiralcraft.util.URIUtil;


import spiralcraft.app.State;

//import spiralcraft.util.RandomUtil;

/**
 * Stores persistent info scoped to a single UI resource 
 *   (ie. normally a server request URI)
 *
 */
public class ResourceSession
{
  public static enum RequestSyncStatus
  {
    RESPONSIVE
    ,INITIATED
    ,OUTOFSYNC
  };
  
  
  private static final ClassLog log
    =ClassLog.getInstance(ResourceSession.class);
  private static final Level debugLevel
    =ClassLog.getInitialDebugLevel(ResourceSession.class, null);
  
//  private final HashMap<String,Action> actionMap
//    =new HashMap<String,Action>();
  
  private final ListMap<String,Action> actionMap
    =new ListMap<String,Action>();
    
  private final VariableMap parameterMap
    =new VariableMap();
    
  private State state;
  
  private String localURI;
  private volatile int sequence;
  private volatile StateFrame currentFrame;
  
  synchronized void clearActions()
  { 
    LinkedList<Action> actions=new LinkedList<Action>();
    actionMap.toValueList(actions);

    // Remove clearable Actions only
    for (Action action:actions)
    {
      if (action.isResponsive())
      { actionMap.remove(action.getName(),action);
      }
    }

    parameterMap.clear();
  }
  
  /**
   * <p>Indicate whether the "lrs" or "last request state" provided in the 
   *   request query synchronizes with this ResourceSession
   * </p>
   *   
   * @param query
   */
  RequestSyncStatus getRequestSyncStatus(VariableMap query)
  {
    if (query==null)
    { 
      if (debugLevel.canLog(Level.TRACE))
      { log.trace("Request is non-resposive because query is null");
      }
      return RequestSyncStatus.INITIATED;
    }
    
    String state=query.getFirst("lrs");
    if (Integer.toString(sequence).equals(state))
    { return RequestSyncStatus.RESPONSIVE;
    }
    else
    { 
      if (debugLevel.canLog(Level.TRACE))
      { log.trace("Request is non-responsive- ?lrs="+state+" != "+sequence);
      }
      
      if (state==null || state.isEmpty())
      { return RequestSyncStatus.INITIATED;
      }
      else
      { return RequestSyncStatus.OUTOFSYNC;
      }
    }
  }
  
  StateFrame currentFrame()
  { return currentFrame;
  }
  
  StateFrame nextFrame()
  { 
    sequence++;
    currentFrame=new StateFrame();
    return currentFrame;
  }
  
  void clearParameters()
  { parameterMap.clear();
  }
  
  public List<Action> getActions(String name)
  { return actionMap.get(name);
  }
  
  public State getRootState()
  { return state;
  }
  
  void setRootState(State state)
  { this.state=state;
  }
  
  /**
   * The URI that will prefix the self-referencing URLs generated by 
   *   registerAction(). Usually set to the HttpServletRequest.requestURI.
   * 
   * @param localURI
   */
  void setLocalURI(String localURI)
  { this.localURI=localURI;
  }
  
  /**
   * The URI that will prefix the self-referencing URLs generated by 
   *   registerAction(). Usually set to the HttpServletRequest.requestURI.
   */
  public String getLocalURI()
  { return localURI;
  }
  
  
  String getAsyncURL()
  {
    String encodedParameters=parameterMap.generateEncodedForm();
    return localURI
      +"?lrs="+Integer.toString(sequence)
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
  String registerAction(Action action)
  {     
    if (debugLevel.canLog(Level.FINE))
    {
      log.fine
        ("Registering action "+action.getName()+"="+action
        +"  parameters="+parameterMap
        );
    }
    actionMap.add(action.getName(),action);
    String encodedParameters=parameterMap.generateEncodedForm();
    return localURI
      +"?action="+action.getName()
      +"&lrs="+Integer.toString(sequence)
      +(encodedParameters!=null?"&"+encodedParameters:"")
      ;
    
  }
  
  /**
   * @return A link back to the current resource that contains published
   *  parameters and maintains the conversation state
   */
  String getAbsoluteBackLink(HttpServletRequest request)
  {
    String encodedParameters=parameterMap.generateEncodedForm();
    URI requestURI=URI.create(request.getRequestURL().toString());
    
    URI backLink
      =URIUtil.replaceRawQuery
        (requestURI
        ,"lrs="+Integer.toString(sequence)
        +(encodedParameters!=null?"&"+encodedParameters:"")
        );
    return backLink.toString();
    
  }
  
  String getEncodedParameters()
  { return parameterMap.generateEncodedForm();
  }

  public void setActionParameter(String name,String[] values)
  { setActionParameter(name,Arrays.asList(values));
  }
  
  public void setActionParameter(String name, List<String> values)
  { 
    if (values!=null)
    { 
      parameterMap.put(name,values);
      if (debugLevel.canLog(Level.FINE))
      { 
        log.fine
          ("Added actionParameter "+name+"="+values+" to map "+parameterMap);
      }
    }
    else
    { parameterMap.remove(name);
    }
  }
  
}
