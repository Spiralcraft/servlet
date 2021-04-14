package spiralcraft.servlet.rpc;

import spiralcraft.vfs.Resource;
import spiralcraft.vfs.util.ByteArrayResource;

public class Response
{
  Integer status=null;
  String contentType=null;
  Resource result;
  
  public Response()
  {
  }
  
  public Response(int status,String text)
  { 
    setStatus(status);
    setText(text);
  }
  
  public void setStatus(Integer status)
  { this.status=status;
  }

  public void setText(String text)
  { 
    this.result=new ByteArrayResource(text.getBytes());
    if (this.contentType==null)
    { this.contentType="text/plain; charset=UTF-8";
    }
  }
}
