package spiralcraft.servlet.rpc;

import java.io.IOException;

import spiralcraft.command.Command;
import spiralcraft.common.ContextualException;
import spiralcraft.json.FromJson;
import spiralcraft.json.JsonException;
import spiralcraft.json.ToJson;
import spiralcraft.lang.Channel;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.servlet.rpc.kit.AbstractHandler;
import spiralcraft.task.Scenario;
import spiralcraft.task.Task;
import spiralcraft.task.TaskCommand;
import spiralcraft.text.ParseException;

/**
 * A handler which marshalls the arguments and result of a spiralcraft.task
 * 
 * @author mike
 *
 */
public class JsonTaskHandler<Tcontext,Tresult>
  extends AbstractHandler
{

  protected Scenario<Tcontext,Tresult> task;
  private ThreadLocalChannel<Command<Task,Tcontext,Tresult>> commandLocal;
  private ThreadLocalChannel<byte[]> input
    =new ThreadLocalChannel<byte[]>(BeanReflector.<byte[]>getInstance(byte[].class));
  private Channel<Tcontext> jsonInput;
  private Channel<String> jsonOutput;
  private ClassLog log=ClassLog.getInstance(getClass());
  
  
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

        jsonInput=new FromJson<Tcontext,byte[]>(contextFocus.getSubject().getReflector())
            .bindChannel(input, contextFocus, null);
        Focus<Tresult> resultFocus=(Focus<Tresult>) focus.chain(commandLocal.resolve(focus,"result",null));
        jsonOutput=new ToJson<Tresult>()
            .bindChannel(resultFocus.getSubject(),resultFocus,null);
        
        
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
  
  
  @Override
  public void handle()
  {
    Response response=call.get().response;
    if (task!=null)
    { 
      TaskCommand<Tcontext,Tresult> command=task.command();
      try
      { input.push(call.get().request.getContentBytes());
      }
      catch (IOException x)
      { 
        response.setStatus(500);
        response.setText("Error reading request content");
        return;
      }
      commandLocal.push(command);
      try
      {
        
        command.setContext(jsonInput.get());
        command.execute();
        call.get().response.setText(jsonOutput.get());
      }
      catch (AccessException x)
      { 
        if (x.unwrapCause() instanceof JsonException)
        {
          JsonException jx=(JsonException) x.unwrapCause();
          if (jx.getCause() instanceof ParseException)
          { 
            response.setStatus(400);
            response.setText(jx.getCause().getMessage());
          }
          
        }
        else
        { 
          log.log(Level.WARNING,"Unhandled exception",x);
          response.setStatus(500);
          response.setText("Server error processing request");
        
        }
      
      }
      finally
      {
        input.pop();
        commandLocal.pop();
      }
    }
  }

}