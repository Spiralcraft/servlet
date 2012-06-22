package spiralcraft.servlet.kit;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import spiralcraft.util.URIUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;
import spiralcraft.vfs.file.FileResource;
import spiralcraft.vfs.url.URLResource;

/**
 * Augments the interface between a Servlet or Filter and the ServletContext
 */
public class ContextAdapter
{

  private final ServletContext context;
  
  public ContextAdapter(ServletContext context)
  { this.context=context;
  
  }

  public ServletContext getContext()
  { return context;
  }
  
  /**
   * Return the path within the servlet context of the request. Effectively
   *   concatenates the servletPath+pathInfo
   */
  public String getRelativePath(HttpServletRequest request)
  { 
    String servletPath=request.getServletPath();
    String pathInfo=request.getPathInfo();
    
    String combinedPath=servletPath==null?"":servletPath;
    if (pathInfo!=null)
    { combinedPath=servletPath+pathInfo;
    }
    return combinedPath;
  }

  /**
   * <p>Return a spiralcraft.vfs.Resource that provides access to a
   *   resource relative to the ServletContext, whether it exists or not.
   * </p>
   */
  public Resource getResource(String contextRelativePath)
    throws ServletException
  { 
    
    String path=context.getRealPath(contextRelativePath);
    
    if (path!=null)
    { return new FileResource(new File(path).getAbsoluteFile());
    }

    
    // Fallback case: Handle VFS based doc root
    
    try
    {
      URI rootURI=(URI) context.getAttribute("spiralcraft.context.root.uri");
      if (rootURI!=null)
      { 
        // context.log("Resolving "+contextRelativePath+" in "+rootURI);
        return Resolver.getInstance().resolve
          (URIUtil.addPathSegment(rootURI,contextRelativePath.substring(1)));
      }
    }
    catch (UnresolvableURIException x)
    {
      throwServletException
        ("Error getting resource ["+contextRelativePath+"]",x);
    }
    
    // Fallback case: Use getResource()
    //
    // Sometimes web servers will not return a URL from getResource()
    //   if the resource doesn't exist already
    //
    
    URL url=null;
    try
    {
      url=context.getResource(contextRelativePath);
    }
    catch (MalformedURLException x)
    { 
      throwServletException
        ("Error getting resource ["+contextRelativePath+"]:",x);
    }
    
    if (url==null)
    { 
      throw new ServletException
        ("ServletContext returned null for getResource() called with "
        +"'"+contextRelativePath+"'"
        );
    }
    
    URI uri=null;
    try
    { uri=url.toURI();
    }
    catch (URISyntaxException x)
    {
      throwServletException
        ("Error getting resource ["+contextRelativePath+"]:"+x,x);
    }
    
    try
    { return Resolver.getInstance().resolve(uri);
    }
    catch (UnresolvableURIException x)
    {
      // Fallback for unknown schemes, etc.
      return new URLResource(url);
    }
  }
  
  protected void throwServletException(String message,Throwable cause)
    throws ServletException
  {
    ServletException x=new ServletException(message);
    if (cause!=null)
    { x.initCause(cause);
    }
    throw x;
  }  
  
}
