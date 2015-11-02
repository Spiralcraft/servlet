package spiralcraft.servlet.rpc;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.io.OutputStream;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.net.mime.MimeHeader;
import spiralcraft.net.mime.MimeHeaderMap;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.servlet.autofilter.AutoFilter;
import spiralcraft.servlet.autofilter.PathContext;

import spiralcraft.servlet.kit.HttpFocus;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.util.ByteArrayResource;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>A Filter that handles requests by mapping them to a programmatically 
 *   defined set of functions.
 * </p>   
 * 
 * 
 * @author mike
 *
 */
public class Filter
  extends AutoFilter
{

  
  private Focus<?> focus;
  private HttpFocus<?> httpFocus;
  private Channel<PathContext> pathContext;
  private volatile boolean initialized;
  private Binding<Void> defaultX;
  private ThreadLocalChannel<Call> callContext
    =new ThreadLocalChannel<Call>
      (BeanReflector.<Call>getInstance(Call.class));
  private Handler[] handlers;
  private HashMap<String,Handler> handlerMap;
  private MimeHeaderMap headers=new MimeHeaderMap();
  
  
  /**
   * Default functionality to invoke if nothing else is mapped to the request
   * 
   * @param defaultX
   */
  public void setDefaultX(Binding<Void> defaultX)
  { this.defaultX=defaultX;
  }
  
  public void setHandlers(Handler[] handlers)
  { 
    this.handlers=handlers;
    handlerMap=new HashMap<String,Handler>();
    for (Handler handler:handlers)
    { handlerMap.put(handler.getName(),handler);
    }
  }
  
  public void setHeaders(MimeHeader[] headers)
  {
    for (MimeHeader header: headers)
    { this.headers.add(header);
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private final void init
    (ServletRequest request
    ,ServletResponse response
    )
    throws ContextualException
  {
  
    Focus<?> requestFocus
      =FocusFilter.getFocusChain((HttpServletRequest) request);
    
    pathContext
      =LangUtil.<PathContext>assertChannel(PathContext.class,requestFocus);
    
    // Create our own Focus, using the 'parent' Focus.
    if (focus==null)
    {         
      if (requestFocus==null)
      { requestFocus=new SimpleFocus<Void>(null);
      }
      
      if (requestFocus.findFocus
            (URI.create("class:/javax/servlet/http/HttpServletRequest")
            ) ==null
         )
      { 
        httpFocus=new HttpFocus<Void>(requestFocus);
        requestFocus=httpFocus;
        httpFocus.push
          (config.getServletContext()
          ,(HttpServletRequest) request
          ,(HttpServletResponse) response
          );

      }
      try
      {
        requestFocus=requestFocus.chain(requestFocus.getSubject());
        requestFocus.addFacet(requestFocus.chain(new SimpleChannel(this,true)));
        requestFocus.addFacet(requestFocus.chain(callContext));

//        requestFocus=bindImports(requestFocus);
        focus=requestFocus;
        Focus exportFocus=focus;
        
        if (defaultX!=null)
        { defaultX.bind(exportFocus);
        }
        
        if (handlers!=null)
        { 
          for (Handler handler:handlers)
          { handler.bind(exportFocus);
          }
        }
              
      }
      finally
      {
        if (httpFocus!=null)
        { httpFocus.pop();
        }
      }
    }
  }
  
  private void push(HttpServletRequest request, HttpServletResponse response)
  { callContext.push(new Call());
  }

  private void pop(HttpServletRequest request)
  { callContext.pop();
  }

  @Override
  public void doFilter(
    ServletRequest request,
    ServletResponse response,
    FilterChain chain)
      throws IOException,
      ServletException
  {
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    boolean pushed=false;
    boolean httpPushed=false;
    
    try
    {
      if (!initialized)
      { 
        synchronized (this)
        { 
          if (!initialized)
          { init(request,response);
          }
        }
      }
      
      if (focus==null)
      { init(request,response);
      }
      
      if (httpFocus!=null)
      { 
        httpFocus.push
          (config.getServletContext()
          ,httpRequest
          ,httpResponse
          );
        httpPushed=true;
      }
      
      push(httpRequest,httpResponse);
      pushed=true;
      
      log.fine("Servicing "+httpRequest.getRequestURI());
      
      Call call=callContext.get();
      if (request.getContentLength()>0)
      {
        call.request.content
          =new ByteArrayResource();
        InputStream in=request.getInputStream();
        OutputStream out=call.request.content.getOutputStream();
        long count=StreamUtil.copyRaw(in, out, 8192, request.getContentLength());
        out.flush();
        out.close();
        log.fine("Read content: "+count);
      }
      
      for (List<MimeHeader> headerList: headers.values())
      { 
        for (MimeHeader header:headerList)
        { httpResponse.setHeader(header.getName(),header.getRawValue());
        }
      }
      
      boolean handled=false;
      
      String handlerName=pathContext.get().getNextPathInfo();
      if (handlerName!=null && handlerMap!=null)
      {
        Handler handler=handlerMap.get(handlerName);
        if (handler!=null)
        { 
          handler.handle();
          handled=true;
        }
      }
      
      if (!handled && defaultX!=null)
      { 
        defaultX.get();
        handled=true;
      }
      
      if (!handled)
      {
        call.response.status=404;
        call.response.setText
          ("404 Not Found. Could not resolve path "
           +pathContext.get().getPathInfo()
           );
      }
      
      if (call.response.status==null)
      { call.response.status=200;
      }
      ((HttpServletResponse) response).setStatus(call.response.status);
      
      if (call.response.contentType!=null)
      { response.setContentType(call.response.contentType);
      }
      
      if (call.response.result!=null)
      { 
        response.setContentLength
          (Long.valueOf(call.response.result.getSize()).intValue()); 

        InputStream in=call.response.result.getInputStream();
        try
        { 
          StreamUtil.copyRaw(in, response.getOutputStream(), 8192);
          in.close();
        }
        finally
        { in.close();
        }
      }
      
    }
    catch (ContextualException x)
    { 
      ServletException sx=new ServletException(x.toString());
      sx.initCause(x);
      throw sx;
    }
    finally
    { 
      if (pushed)
      { 
        // If we changed this Thread's Focus subject, put it back
        pop((HttpServletRequest) request);
      }
      if (httpFocus!=null && httpPushed)
      { httpFocus.pop();
      }
      
    }
  }
  
  
}