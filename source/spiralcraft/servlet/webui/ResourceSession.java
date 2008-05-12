package spiralcraft.servlet.webui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import spiralcraft.log.ClassLogger;
import spiralcraft.net.http.VariableMap;
import spiralcraft.textgen.ElementState;


//import spiralcraft.util.RandomUtil;

/**
 * Stores persistent info scoped to a single UI resource 
 *   (ie. normally a server request URI)
 *
 */
public class ResourceSession
{
  private static final ClassLogger log
    =ClassLogger.getInstance(ResourceSession.class);
  
  private final HashMap<String,Action> actionMap
    =new HashMap<String,Action>();
    
  private final VariableMap parameterMap
    =new VariableMap();
    
  private ElementState state;
  
  private String localURI;
  private boolean debug;
  
  void clearActions()
  { 
    actionMap.clear();
    parameterMap.clear();
  }
  
  void clearParameters()
  { parameterMap.clear();
  }
  
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  public Action getAction(String name)
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
  String registerAction(Action action,String preferredName)
  { 
    if (preferredName==null)
    { preferredName=Integer.toString(actionMap.size());
    }
    
    if (debug)
    {
      log.fine
        ("Registering action "+preferredName+"="+action
        +"  parameters="+parameterMap
        );
    }
    actionMap.put(preferredName,action);
    String encodedParameters=parameterMap.generateEncodedForm();
    return localURI
      +"?action="+preferredName
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
