package spiralcraft.servlet.rpc;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import spiralcraft.vfs.Resource;
import spiralcraft.vfs.StreamUtil;

public class Request
{
  
  Resource content;
  HttpServletRequest hsr;
  
  public String getURI()
  { return hsr.getRequestURI();
  }
  
  public HttpSession getSession(boolean create)
  { return hsr.getSession(create);
  }
  
  public Resource getContent()
  { return content;
  }

  public byte[] getContentBytes()
    throws IOException
  { 
    if (content==null)
    { return null;
    }
    InputStream in = content.getInputStream();
    try
    { return StreamUtil.readBytes(in);
    }
    finally
    { in.close();
    }
  }

}