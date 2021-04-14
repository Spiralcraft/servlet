package spiralcraft.servlet.rpc;

import spiralcraft.common.declare.Declarable;
import spiralcraft.common.declare.DeclarationInfo;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;

/**
 * Encapsulates a condition in a set of fall-through conditions that must
 *   succeed in order for an action to continue. A failed 
 * 
 * @author mike
 *
 */
public class Guard
  implements Declarable
{ 
  private static final ClassLog log=ClassLog.getInstance(Guard.class);
  
  private DeclarationInfo declarationInfo;
  
  private Binding<Boolean> assertion;
  private Binding<Response> denialResponse;
  private Binding<?> onFailure;
  private Response defaultResponse=new Response(400,"Unable to service request");
  
  public void bind(Focus<?> focus) 
      throws BindException
  {
    if (assertion!=null)
    { assertion.bind(focus);
    }
    if (denialResponse!=null)
    { denialResponse.bind(focus);
    }
    if (onFailure!=null)
    { onFailure.bind(focus);
    }
    
  }
  
  /**
   * Check for a non-null guard response. Execute onFailure if failed. 
   * 
   * @return
   */
  public Response check()
  {
    try
    { 
      if (assertion!=null)
      {
        if (Boolean.TRUE.equals(assertion.get()))
        { return null;
        }
        if (onFailure!=null)
        { onFailure.get();
        }
        if (denialResponse!=null)
        { return denialResponse.get();
        }
        else
        { return defaultResponse;
        }
      }
      else
      {
        if (denialResponse!=null)
        {
          Response r=denialResponse.get();
          if (r!=null)
          { onFailure.get();
          }
          return r;
        }
        // Always fail if nothing is configured
        return defaultResponse;
      }
      
    }
    catch (Exception x)
    { log.log(Level.WARNING,"Error checking assertion "+getDeclarationInfo(),x);
    }
    return null;
  }

  /**
   * An assertion to be performed. If the assertion does not return Boolean.TRUE
   *   or has not been specified, a response will be evaluated.
   */
  public void setAssertion(Binding<Boolean> assertion)
  { this.assertion=assertion;
  }

  /**
   * An action to take when the assertion fails.
   * 
   * @param onFailure
   */
  public void setOnFailure(Binding<?> onFailure)
  { this.onFailure=onFailure;
  }

  /**
   * Determines which response to return when the assertion fails or has not been
   *   specified.
   * 
   * @param failureResponse
   */
  public void setDenialResponse(Binding<Response> denialResponse)
  { this.denialResponse=denialResponse;
  }

  /**
   * A response to use when the assertion fails and denialResponse has not been
   *   specified
   * 
   * @param r
   */
  public void setDefaultResponse(Response r)
  { this.defaultResponse=r;
  }

  @Override
  public void setDeclarationInfo(
    DeclarationInfo declarationInfo)
  { this.declarationInfo=declarationInfo;
  }

  @Override
  public DeclarationInfo getDeclarationInfo()
  { return this.declarationInfo;
  }
}