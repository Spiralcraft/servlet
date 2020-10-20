package spiralcraft.servlet.rpc;

import spiralcraft.util.Path;

public class SubCall
  implements Call
{

  
  private final Call parent;
  private final String pathInfo;
  private final Path nextPath;
  
  public SubCall(Path path,String pathInfo,Call parent)
  { 
    this.parent=parent;
    this.nextPath=path;
    this.pathInfo=pathInfo;
  }

  @Override
  public String getPathInfo()
  { return pathInfo;
  }

  @Override
  public Path getNextPath()
  { return nextPath;
  }

  @Override
  public void respond(
    int statusCode,
    String message)
  { parent.respond(statusCode,message);
  }

  @Override
  public Request getRequest()
  { return parent.getRequest();
  }
}