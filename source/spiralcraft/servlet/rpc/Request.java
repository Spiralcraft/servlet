package spiralcraft.servlet.rpc;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import spiralcraft.net.http.VariableMap;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.StreamUtil;

public class Request
{
  public Resource content;
  public final HttpServletRequest hsr;

  public Request(HttpServletRequest hsr)
  { this.hsr=hsr;
  }
  
  public String getURI()
  { return hsr.getRequestURI();
  }
  
  public HttpSession getSession(boolean create)
  { return hsr.getSession(create);
  }
  
  public Resource getContent()
  { return content;
  }
  
  public boolean isGet()
  { return hsr.getMethod().equals("GET");
  }

  public boolean isPost()
  { return hsr.getMethod().equals("POST");
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
  
  public VariableMap getQueryParameters()
  { 
    String queryString=hsr.getQueryString();
    if (queryString!=null && queryString.length()>0)
    { return VariableMap.fromUrlEncodedString(queryString);
    }
    else 
    { return null;
    }
  }

}