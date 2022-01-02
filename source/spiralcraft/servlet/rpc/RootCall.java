package spiralcraft.servlet.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import spiralcraft.util.Path;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.util.ByteArrayResource;

public class RootCall
  implements Call
{
  final Response response;
  final Request request;
  final Path nextPath;
  final String pathInfo;
  
  public RootCall(HttpServletRequest request,String pathInfo)
  { 
    this.request=new Request(request);
    this.response=new Response();
    this.pathInfo=pathInfo;
    int query=pathInfo.indexOf('?');
    if (query<0)
    { nextPath=new Path(pathInfo);
    }
    else
    { nextPath=new Path(pathInfo.substring(0,query));
    }
  }
  
  @Override
  public Path getNextPath()
  { return nextPath;
  }
  
  @Override
  public String getPathInfo()
  { return pathInfo;
  }
  
  public Request getRequest()
  { return request;
  }
  
  public Response getResponse()
  { return response;
  }
  
  void init(HttpServletRequest request)
    throws IOException
  {

  }
  
  public void respond(int statusCode,String message)
  { 
    response.setText(message);
    response.setStatus(statusCode);
  }
  
  public void respond(Response r)
  { 
    response.status=r.status;
    response.contentType=r.contentType;
    response.result=r.result;
  }
  
}
