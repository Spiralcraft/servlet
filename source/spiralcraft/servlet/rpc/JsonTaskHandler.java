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
import spiralcraft.log.Level;
import spiralcraft.rules.RuleException;
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
    if (task!=null)
    { 
      TaskCommand<Tcontext,Tresult> command=task.command();
      try
      { input.push(call.get().getRequest().getContentBytes());
      }
      catch (IOException x)
      { 
        call.get().respond(500,"Error reading request content");
        return;
      }
      commandLocal.push(command);
      try
      {
        if (input.get()!=null)
        { command.setContext(jsonInput.get());
        }
        command.execute();
        if (command.getException()!=null)
        { 
          Throwable exception=command.getException();
          if (exception instanceof ContextualException)
          { exception=((ContextualException) exception).getRootCause();
          }
          
          if (exception instanceof RuleException)
          {
            String message=exception.getMessage();

            log.log(Level.WARNING
              ,declarationInfo+": Rule violation "+message);            
            call.get().respond(422,message);
          }
          else
          {
            log.log(Level.WARNING,declarationInfo+": Command threw exception",command.getException());
            call.get().respond(500,"Server error processing request");
          }
        }
        else if (command.getResult()!=null)
        { call.get().respond(200,jsonOutput.get());
        }
      }
      catch (AccessException x)
      { 
        if (x.unwrapCause() instanceof JsonException)
        {
          JsonException jx=(JsonException) x.unwrapCause();
          if (jx.getCause() instanceof ParseException)
          { call.get().respond(400,jx.getCause().getMessage());
          }
          else
          {
            log.log(Level.WARNING,declarationInfo+": Unhandled exception",x);
            call.get().respond(500,"Server error processing request");
          }
          
        }
        else
        { 
          log.log(Level.WARNING,declarationInfo+": Unhandled exception",x);
          call.get().respond(500,"Server error processing request");
        
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