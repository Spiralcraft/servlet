package spiralcraft.servlet.rpc.kit;


import java.io.IOException;

import spiralcraft.common.ContextualException;
import spiralcraft.common.declare.Declarable;
import spiralcraft.common.declare.DeclarationInfo;
import spiralcraft.lang.Focus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.servlet.rpc.Handler;
import spiralcraft.servlet.rpc.Response;
import spiralcraft.servlet.rpc.Call;
import spiralcraft.servlet.rpc.Guard;

/**
 * Provides a base implementation for other handlers
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
  
  protected Guard[] guard;
  
  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
      throws ContextualException
  { 
    focusChain=focusChain.chain(new SimpleChannel<Handler>(this,true));
    focusChain.addFacet(focusChain.chain(call));
    if (guard!=null)
    { 
      for (Guard g:guard)
      { g.bind(focusChain);
      }
    }
    
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

  public void setGuard(Guard[] guard)
  { this.guard=guard;
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
    {
      push();
      try
      { 
        if (checkGuard(callObject))
        { this.handle();
        }
      }
      finally
      { 
        pop();
      }
    }
    catch (Exception x)
    { 
      call.get().respond(500,"Error handling request");
      log.log(Level.WARNING,getDeclarationInfo()+": Error handling request",x);
    }
    finally
    { call.pop();
    }
  }
  
  protected final boolean checkGuard(Call call)
  { 
    if (guard!=null)
    { 
      for (Guard g: guard)
      { 
        Response r=g.check();
        if (r!=null)
        { 
          call.respond(r);
          return false;
        }
      }
    }
    return true;
    
  }

  protected void push()
    throws Exception
  {
  }
  
  protected void pop()
  {
  }
  
  protected abstract void handle();
}