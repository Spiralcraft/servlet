package spiralcraft.servlet.rpc;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;

import spiralcraft.common.ContextualException;
import spiralcraft.common.DynamicLoadException;
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
  private Handler defaultHandler;
  private MimeHeaderMap headers=new MimeHeaderMap();
  private boolean reflectOriginHeader;
  private boolean requireHandler;
  private HashSet<String> forwardList=new HashSet<>();
  
  
  /**
   * Default functionality to invoke if nothing else is mapped to the request
   * 
   * @param defaultX
   */
  public void setDefaultX(Binding<Void> defaultX)
  { this.defaultX=defaultX;
  }
  
  /**
   * Default handler if no specific handlers are  mapped to the request
   * 
   * @param defaultX
   */
  public void setDefaultHandler(Handler defaultHandler)
  { this.defaultHandler=defaultHandler;
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
  
  public void setForward(String[] forward)
  {
    for (String s:forward)
    { forwardList.add(s);
    }
  }
  
  /**
   * Reflects the "Origin" request header to the "Access-Control-Allow-Origin"
   *   response header to allow cookies and credentials to be passed in CORS
   *   requests.
   * @param reflectOrigin
   */
  public void setReflectOriginHeader(boolean reflectOriginHeader)
  { this.reflectOriginHeader=reflectOriginHeader;
  }

  /**
   * When set, a "404 NOT FOUND" response will be issued when a handler is
   *   not found for the request. Otherwise, the request will be passed through
   *   the filter chain if a handler is not found.
   * 
   * @param requireHandler
   */
  public void setRequireHandler(boolean requireHandler)
  { this.requireHandler=requireHandler;
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

        if (defaultHandler!=null)
        { defaultHandler.bind(exportFocus);
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
  { 
  }

  private void pop(HttpServletRequest request)
  { 
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
          { 
            try
            { init(request,response);
            }
            catch (ContextualException x)
            { 
              throw new DynamicLoadException
                ("Error loading RPC Filter",getDeclarationInfo(),x);
            }
          }
        }
      }
      
      if (focus==null)
      { 
        try
        { init(request,response);
        }
        catch (ContextualException x)
        { 
          throw new DynamicLoadException
            ("Error loading RPC Filter",getDeclarationInfo(),x);
        }
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
      
      PathContext pc=pathContext.get();
      RootCall call=new RootCall(httpRequest,pc.getPathInfo());
      if (debug)
      { log.fine("PathContext.getPathInfo(): "+pc.getPathInfo());
      }
      callContext.push(call);
      push(httpRequest,httpResponse);
      pushed=true;
      
      for (List<MimeHeader> headerList: headers.values())
      { 
        for (MimeHeader header:headerList)
        { httpResponse.setHeader(header.getName(),header.getRawValue());
        }
      }
      if (reflectOriginHeader)
      { 
        String origin=httpRequest.getHeader("Origin");
        if (origin!=null)
        { httpResponse.setHeader("Access-Control-Allow-Origin", origin);
        }
      }
      
      boolean handled=false;
      
      String handlerName=pathContext.get().getNextPathInfo();
      if (debug)
      { log.fine("Looking for handler for name ["+handlerName+"]");
      }
      
      Handler handler=null;
      Call subcall=call;
      if (!forwardList.contains(handlerName))
      {
        if (handlerMap!=null)
        { 
          handler=handlerMap.get(handlerName);
          
          if (handler==null 
              && handlerName!=null 
              && !handlerName.isEmpty()
              )
          { handler=handlerMap.get("*");
          }

          if (handler!=null)
          { 
            String newPathInfo=call.getPathInfo().substring(handlerName.length());
            if (newPathInfo.startsWith("/"))
            { newPathInfo=newPathInfo.substring(1);
            }
            subcall=new SubCall
              (call.getNextPath()
              , newPathInfo
              , call
              );
          }
          
        }
         
        
        if (handler==null)
        { handler=defaultHandler;
        }
      }
      
      if (handler!=null)
      { 
        if (debug)
        { log.fine("Servicing "+httpRequest.getRequestURI());
        }
        call.init(httpRequest);
        handler.handle(subcall);
        handled=true;
      }
      
      if (!handled && defaultX!=null)
      { 
        if (debug)
        { log.fine("Servicing "+httpRequest.getRequestURI()+" with default hook");
        }
        call.init(httpRequest);
        defaultX.get();
        handled=true;
      }
      
      if (!handled && !requireHandler)
      { 
        if (debug)
        { log.fine("No handler for name ["+handlerName+"], passing through");
        }
        chain.doFilter(request,response);
      }
      else
      {
        if (!handled)
        {
          call.init(httpRequest);
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
        callContext.pop();
      }
      if (httpFocus!=null && httpPushed)
      { httpFocus.pop();
      }
      
    }
  }
  
  
}