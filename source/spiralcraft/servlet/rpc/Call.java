package spiralcraft.servlet.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.util.ByteArrayResource;

public class Call
{
  public final Response response=new Response();
  public final Request request=new Request();
  
  void init(HttpServletRequest request)
    throws IOException
  {
    if (request.getContentLength()>0)
    {
      this.request.content
        =new ByteArrayResource();
      InputStream in=request.getInputStream();
      OutputStream out=this.request.content.getOutputStream();
      StreamUtil.copyRaw(in, out, 8192, request.getContentLength());
      out.flush();
      out.close();
    }
  }
  
  public void respond(int statusCode,String message)
  { 
    response.setText(message);
    response.setStatus(statusCode);
  }
  
}
