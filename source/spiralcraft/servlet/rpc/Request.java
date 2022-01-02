package spiralcraft.servlet.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import spiralcraft.net.http.MultipartVariableMap;
import spiralcraft.net.http.VariableMap;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.util.ByteArrayResource;

public class Request
{
  private Resource content;
  private boolean contentRead;
  public final HttpServletRequest hsr;
  public VariableMap post;
  
  public Request(HttpServletRequest hsr)
  { this.hsr=hsr;
  }
  
  public String getURI()
  { return hsr.getRequestURI();
  }
  
  public HttpSession getSession(boolean create)
  { return hsr.getSession(create);
  }

  /**
   * Parse request bodies with relevant content types as variable maps
   * 
   * @return
   */
  public VariableMap getPostAsVars()
    throws IOException
  {
    if (post!=null)
    { return post;
    }
    String mainContentType=hsr.getContentType();
    if (mainContentType.equalsIgnoreCase("application/x-www-form-urlencoded"))
    {
      post = new VariableMap();
      if (hsr.getContentLength()>0)
      { 
        String content= StreamUtil.readString
            (hsr.getInputStream(), hsr.getContentLength(),StandardCharsets.UTF_8);
        post.parseEncodedForm(content);
      }
      return post;
    }
    else if (mainContentType.startsWith("multipart/form-data"))
    {
      post = new MultipartVariableMap(hsr.getInputStream(),hsr.getContentType(),hsr.getContentLength());
      return post;
    }
    else
    { return null;
    }
    
  }
  
  public Resource getContent()
    throws IOException
  { 
    if (!contentRead)
    { readContent();
    }
    return content;
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
    Resource content=getContent();
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

  private void readContent()
    throws IOException
  {
    contentRead=true;
    if (hsr.getContentLength()>0)
    {
      content
        =new ByteArrayResource();
      InputStream in=hsr.getInputStream();
      OutputStream out=content.getOutputStream();
      StreamUtil.copyRaw(in, out, 8192, hsr.getContentLength());
      out.flush();
      out.close();
    }  
  }
}