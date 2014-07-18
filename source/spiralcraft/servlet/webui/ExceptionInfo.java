package spiralcraft.servlet.webui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import spiralcraft.common.ContextualException;
import spiralcraft.servlet.webui.textgen.UIResourceUnit;
import spiralcraft.vfs.Resource;

public class ExceptionInfo
{
  public final Resource resource;
  public final String instancePath;
  public final UIResourceUnit resourceUnit;
  public final Throwable exception;
  public final Throwable[] exceptions;
  public final Object[] contexts;
  public final String stackTrace;

  public ExceptionInfo
    (Resource resource
    ,String instancePath
    ,UIResourceUnit resourceUnit
    ,Throwable exception
    )
  {
    this.resource=resource;
    this.instancePath=instancePath;
    this.resourceUnit=resourceUnit;
    this.exception=exception;
    
    StringWriter buf=new StringWriter();
    exception.printStackTrace(new PrintWriter(buf));
    stackTrace=buf.toString();
        
    ArrayList<Throwable> exceptionList=new ArrayList<>();
    while (exception!=null)
    {
      exceptionList.add(exception);
      exception=exception.getCause();
    }
    this.exceptions=exceptionList.toArray(new Throwable[exceptionList.size()]);
    
    ArrayList<Object> contexts=new ArrayList<>();
    for (Throwable t:exceptions)
    {
      if (t instanceof ContextualException)
      {
        Object context=((ContextualException) t).getContext();
        if (context!=null)
        { contexts.add(context);
        }
      }
    }
    this.contexts=contexts.toArray(new Object[contexts.size()]);
  }
}
