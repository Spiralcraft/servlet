package spiralcraft.servlet.rpc;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.rpc.kit.AbstractHandler;

/**
 * A handler which evaluates an expression. The expression must explicitly
 *   manipulate the rpc:Response to send something back to the client
 * 
 * @author mike
 *
 */
public class BasicHandler
  extends AbstractHandler
{

  protected Binding<?> x;
  
  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
      throws ContextualException
  { 
    focusChain=super.bind(focusChain);
    try
    {
      if (x!=null)
      { x.bind(focusChain);
      }
    }
    catch (Exception x)
    {
      throw new ContextualException
        ("Error binding rpc Handler",declarationInfo,x);
    }
    return focusChain;
  }


  public void setX(Binding<?> x)
  { this.x=x;
  }
  
  @Override
  public void handle()
  {
    if (x!=null)
    { x.get();
    }
  }

}