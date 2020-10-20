package spiralcraft.servlet.rpc.kit;


import spiralcraft.common.ContextualException;
import spiralcraft.common.declare.Declarable;
import spiralcraft.common.declare.DeclarationInfo;
import spiralcraft.lang.Focus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.servlet.rpc.Handler;
import spiralcraft.servlet.rpc.Call;

/**
 * A handler which evaluates an expression. The expression must explicitly
 *   manipulate the rpc:Response to send something back to the client
 * 
 * @author mike
 *
 */
public abstract class AbstractHandler
  implements Handler,Declarable
{

  protected final ClassLog log
    =ClassLog.getInstance(getClass());
  protected String name;
  protected DeclarationInfo declarationInfo;
  protected final ThreadLocalChannel<Call> call
    =new ThreadLocalChannel<Call>
      (BeanReflector.<Call>getInstance(Call.class));
  
  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
      throws ContextualException
  { 
    focusChain=focusChain.chain(new SimpleChannel<Handler>(this,true));
    focusChain.addFacet(focusChain.chain(call));
    return focusChain;
  }

  @Override
  public void setName(String name)
  { this.name=name;
  }

  @Override
  public String getName()
  { return name;
  }

  @Override
  public void setDeclarationInfo(
    DeclarationInfo declarationInfo)
  { this.declarationInfo=declarationInfo;
  }

  @Override
  public DeclarationInfo getDeclarationInfo()
  { return declarationInfo;
  }
  
  public final void handle(Call callObject)
  {
    call.push(callObject);
    try
    { this.handle();
    }
    finally
    { call.pop();
    }
  }
  
  protected abstract void handle();
}