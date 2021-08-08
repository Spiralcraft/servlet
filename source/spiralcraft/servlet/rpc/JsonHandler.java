package spiralcraft.servlet.rpc;

import spiralcraft.command.Command;
import spiralcraft.common.ContextualException;
import spiralcraft.data.InvalidValueException;
import spiralcraft.data.task.Transaction;
import spiralcraft.json.FromJson;
import spiralcraft.json.JsonException;
import spiralcraft.json.ToJson;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.Level;
import spiralcraft.net.http.VariableMap;
import spiralcraft.net.http.VariableMapBinding;
import spiralcraft.rules.RuleException;
import spiralcraft.servlet.rpc.kit.AbstractHandler;
import spiralcraft.task.Eval;
import spiralcraft.task.Scenario;
import spiralcraft.task.Task;
import spiralcraft.task.TaskCommand;
import spiralcraft.text.ParseException;

/**
 * A handler which accepts parameters and returns a result in the form of a
 *   JSON object
 * 
 * @author mike
 *
 */
public class JsonHandler<Tcontext,Tresult>
  extends AbstractHandler
{

  protected Scenario<Void,Tresult> task;
  private ThreadLocalChannel<Command<Task,Void,Tresult>> commandLocal;
  private ThreadLocalChannel<byte[]> input
    =new ThreadLocalChannel<byte[]>(BeanReflector.<byte[]>getInstance(byte[].class));
  private Channel<Tcontext> jsonInput;
  private Channel<String> jsonOutput;
  private Binding<Tcontext> params;
  private String[] queryParams;
  private VariableMapBinding<?>[] queryBindings;
  private ThreadLocalChannel<Tcontext> paramsLocal;
  private Binding<Tresult> result;
  private boolean transactional;
  
  
  public void setQueryParams(String[] queryParams)
  { this.queryParams=queryParams;
  }

  public void setParams(Binding<Tcontext> params)
  { this.params=params;
  }
  
  public void setResult(Binding<Tresult> result)
  { this.result=result;
  }
  
  public void setTransactional(boolean transactional)
  { this.transactional=transactional;
  }
  
  @SuppressWarnings({"unchecked","rawtypes"})
  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
      throws ContextualException
  { 
    if (params!=null)
    {
      params.bind(focusChain);
      paramsLocal=new ThreadLocalChannel<Tcontext>(params.getReflector(),true);
      focusChain=focusChain.chain(paramsLocal);

      if (queryParams!=null)
      {
        queryBindings=new VariableMapBinding[queryParams.length];
        for (int i=0; i<queryParams.length; i++)
        { 
          String param=queryParams[i];
          Binding paramChan=new Binding(Expression.create(param));
          paramChan.bind(focusChain);
          queryBindings[i]=new VariableMapBinding(paramChan,param,null);
        }
      }
    }
    
    focusChain=super.bind(focusChain);
    
    Eval<Void,Tresult> eval=new Eval<Void,Tresult>();
    eval.setX(result);

    if (transactional)
    {
      Transaction transaction=new Transaction();
      transaction.chain(eval);
      task=(Scenario<Void,Tresult>) transaction;
    }
    else
    { task=eval;
    }
    
    try
    {
          
      task.bind(focusChain);
      commandLocal
        =new ThreadLocalChannel<Command<Task,Void,Tresult>>
          ( (Reflector<Command<Task,Void,Tresult>>) task.getCommandReflector()
          ,true
          );
    
      Focus<Command<Task,Void,Tresult>> focus=
         new SimpleFocus<Command<Task,Void,Tresult>>(focusChain,commandLocal);

      if (params!=null)
      {
        jsonInput=new FromJson<Tcontext,byte[]>
          (params.getReflector())
            .bindChannel(input, focusChain, null);
      }
      Focus<Tresult> resultFocus=(Focus<Tresult>) focus.chain(commandLocal.resolve(focus,"result",null));
      jsonOutput=new ToJson<Tresult>()
          .bindChannel(resultFocus.getSubject(),resultFocus,null);
        
    }
    catch (Exception x)
    {
      throw new ContextualException
        ("Error binding rpc Handler",declarationInfo,x);
    }
    return focusChain;
  }

  protected void push()
    throws Exception
  { 
    if (params!=null)
    {
      input.push(call.get().getRequest().getContentBytes());
      if (input.get()!=null)
      { paramsLocal.push(jsonInput.get());
      }
      else
      { paramsLocal.push(params.get());
      }
      if (queryBindings!=null)
      {
        VariableMap queryParameters=call.get().getRequest().getQueryParameters();
        if (queryParameters!=null)
        {
          for (VariableMapBinding<?> qb : queryBindings)
          { qb.read(queryParameters);
          }
        }
      }
    }
  }
  
  protected void pop()
  { 
    if (params!=null)
    {
      paramsLocal.pop();
      input.pop();
    }
  }
  
  @Override
  protected void handle()
  {
    TaskCommand<Void,Tresult> command=task.command();

    commandLocal.push(command);
    try
    {
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
        else if (exception instanceof AccessException)
        {
          String message=exception.getMessage();

          log.log(Level.WARNING
            ,declarationInfo+": AccessException "+message);            
          call.get().respond(422,message);
        }
        else if (exception instanceof InvalidValueException)
        {
          String message=exception.getMessage();

          log.log(Level.WARNING
            ,declarationInfo+": InvalidValueException "+message);            
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
    catch (Exception x)
    {
      log.log(Level.WARNING,declarationInfo+": Unhandled exception",x);
      call.get().respond(500,"Server error processing request");
    }
    finally
    { commandLocal.pop();
    }
  }

}