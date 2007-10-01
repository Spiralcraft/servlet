package spiralcraft.servlet.webui;

import java.util.HashMap;

import spiralcraft.textgen.ElementState;

//import spiralcraft.util.RandomUtil;

/**
 * Stores persistent info scoped to a single UI resource. 
 *
 */
public class LocalSession
{
  private final HashMap<String,Action> actionMap
    =new HashMap<String,Action>();
    
  private ElementState state;
  
  private String localURI;
  
  void clearActions()
  { actionMap.clear();
  }
  
  
  public Action getAction(String name)
  { return actionMap.get(name);
  }
  
  public ElementState getState()
  { return state;
  }
  
  void setState(ElementState state)
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
    
    actionMap.put(preferredName,action);
    return localURI+"?action="+preferredName;
  }
  
}
