package spiralcraft.servlet.rpc.kit;

import spiralcraft.common.ContextualException;
import spiralcraft.common.declare.Declarable;
import spiralcraft.common.declare.DeclarationInfo;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;
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
  protected Channel<Call> call;
  
  
  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
      throws ContextualException
  { 
    call=LangUtil.assertChannel(Call.class, focusChain);
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
}