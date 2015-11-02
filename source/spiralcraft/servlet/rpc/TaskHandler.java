package spiralcraft.servlet.rpc;

import spiralcraft.command.Command;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.servlet.rpc.kit.AbstractHandler;
import spiralcraft.task.Scenario;
import spiralcraft.task.Task;
import spiralcraft.task.TaskCommand;

/**
 * A handler which marshalls the arguments and result of a spiralcraft.task
 * 
 * @author mike
 *
 */
public class TaskHandler<Tcontext,Tresult>
  extends AbstractHandler
{

  protected Scenario<Tcontext,Tresult> task;
  private ThreadLocalChannel<Command<Task,Tcontext,Tresult>> commandLocal;
  private Binding<Tcontext> inputX;
  private Binding<Tresult> outputX;

  @SuppressWarnings("unchecked")
  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
      throws ContextualException
  { 
    focusChain=super.bind(focusChain);
    try
    {
      if (task!=null)
      { 
        task.bind(focusChain);
        commandLocal
          =new ThreadLocalChannel<Command<Task,Tcontext,Tresult>>
            ( (Reflector<Command<Task,Tcontext,Tresult>>) task.getCommandReflector()
            ,true
            );
      
        Focus<Command<Task,Tcontext,Tresult>> focus
          =new SimpleFocus<Command<Task,Tcontext,Tresult>>(focusChain,commandLocal);
        Focus<Tcontext> contextFocus=(Focus<Tcontext>) focus.chain(commandLocal.resolve(focus,"context",null));
        if (inputX!=null)
        { inputX.bind(contextFocus);
        }
        Focus<Tresult> resultFocus=(Focus<Tresult>) focus.chain(commandLocal.resolve(focus,"result",null));
        if (outputX!=null)
        { outputX.bind(resultFocus);
        }
        
        
      }
    }
    catch (Exception x)
    {
      throw new ContextualException
        ("Error binding rpc Handler",declarationInfo,x);
    }
    return focusChain;
  }


  public void setTask(Scenario<Tcontext,Tresult> task)
  { this.task=task;
  }
  
  public void setInputX(Binding<Tcontext> inputX)
  { this.inputX=inputX;
  }

  public void setOutputX(Binding<Tresult> outputX)
  { this.outputX=outputX;
  }
  
  @Override
  public void handle()
  {
    if (task!=null)
    { 
      TaskCommand<Tcontext,Tresult> command=task.command();
      commandLocal.push(command);
      if (inputX!=null)
      { command.setContext(inputX.get());
      }
      command.execute();
      if (outputX!=null)
      { outputX.get();
      }
    }
  }

}