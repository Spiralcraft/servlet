package spiralcraft.servlet.webui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import spiralcraft.log.ClassLog;
import spiralcraft.net.http.VariableMap;
import spiralcraft.textgen.ElementState;
import spiralcraft.util.ListMap;


//import spiralcraft.util.RandomUtil;

/**
 * Stores persistent info scoped to a single UI resource 
 *   (ie. normally a server request URI)
 *
 */
public class ResourceSession
{
  private static final ClassLog log
    =ClassLog.getInstance(ResourceSession.class);
  
//  private final HashMap<String,Action> actionMap
//    =new HashMap<String,Action>();
  
  private final ListMap<String,Action> actionMap
    =new ListMap<String,Action>();
    
  private final VariableMap parameterMap
    =new VariableMap();
    
  private ElementState state;
  
  private String localURI;
  private boolean debug;
  
  synchronized void clearActions()
  { 
    LinkedList<Action> actions=new LinkedList<Action>();
    actionMap.toValueList(actions);

    // Remove clearable Actions only
    for (Action action:actions)
    {
      if (action.isClearable())
      { actionMap.remove(action.getName(),action);
      }
    }

    parameterMap.clear();
  }
  
  void clearParameters()
  { parameterMap.clear();
  }
  
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  public List<Action> getActions(String name)
  { return actionMap.get(name);
  }
  
  public ElementState getRootState()
  { return state;
  }
  
  void setRootState(ElementState state)
  { this.state=state;
  }
  
  void setLocalURI(String localURI)
  { this.localURI=localURI;
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
    if (debug)
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
      +(encodedParameters!=null?"&"+encodedParameters:"")
      ;
    
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
      if (debug)
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
