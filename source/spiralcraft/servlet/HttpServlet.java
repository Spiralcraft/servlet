package spiralcraft.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for web servlets
 */
public class HttpServlet
  implements Servlet
{
  private ServletConfig _config;

  public static final String METHOD_GET="GET";
  public static final String METHOD_HEAD="HEAD";
  public static final String METHOD_POST="POST";
  public static final String METHOD_PUT="PUT";
  public static final String METHOD_OPTIONS="OPTIONS";
  public static final String METHOD_TRACE="TRACE";
  public static final String METHOD_DELETE="DELETE";
  

  public void init(ServletConfig config)
  { _config=config;
  }
  
  public void destroy()
  {
  }
  
  protected void doGet(HttpServletRequest request,HttpServletResponse response)
  {
  }
  
  protected void doHead(HttpServletRequest request,HttpServletResponse response)
  { 
  }

  protected void doPost(HttpServletRequest request,HttpServletResponse response)
  {
  }

  protected void doPut(HttpServletRequest request,HttpServletResponse response)
  {
  }

  protected void doOptions(HttpServletRequest request,HttpServletResponse response)
  { 
  }

  protected void doTrace(HttpServletRequest request,HttpServletResponse response)
  {
  }

  protected void doDelete(HttpServletRequest request,HttpServletResponse response)
  {
  }

  public void service(ServletRequest request,ServletResponse response)
  {
    HttpServletRequest httpRequest=(HttpServletRequest) request;
    HttpServletResponse httpResponse=(HttpServletResponse) response;
    String method=httpRequest.getMethod().intern();

    if (method==METHOD_GET)
    { doGet(httpRequest,httpResponse);
    }
    else if (method==METHOD_HEAD)
    { doHead(httpRequest,httpResponse);
    }
    else if (method==METHOD_POST)
    { doPost(httpRequest,httpResponse);
    }
    else if (method==METHOD_PUT)
    { doPut(httpRequest,httpResponse);
    }
    else if (method==METHOD_OPTIONS)
    { doOptions(httpRequest,httpResponse);
    }
    else if (method==METHOD_TRACE)
    { doTrace(httpRequest,httpResponse);
    }
    else if (method==METHOD_DELETE)
    { doDelete(httpRequest,httpResponse);
    }
    
  }

  public ServletConfig getServletConfig()
  { return _config;
  }
  
  public String getServletInfo()
  { return getClass().getName();
  }
}
