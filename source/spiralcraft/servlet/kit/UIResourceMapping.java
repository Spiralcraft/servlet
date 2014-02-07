package spiralcraft.servlet.kit;

import java.io.IOException;

import spiralcraft.vfs.Resource;

public class UIResourceMapping
{
  /**
   * Create a new UIResourceMapping if the specified resource exists,
   *   or return null if it doesn't.
   *   
   * @param mappedPath
   * @param resource
   * @return 
   * @throws IOException 
   */
  public static final UIResourceMapping 
    forResource(String mappedPath,Resource resource) throws IOException
  {
    if (resource==null || !resource.exists())
    { return null;
    }
    return new UIResourceMapping(mappedPath,resource);
  }
  
  public final String mappedPath;
  public final Resource resource;
  
  public UIResourceMapping(String mappedPath,Resource resource)
  { 
    this.mappedPath=mappedPath;
    this.resource=resource;
  }
  
  @Override
  public String toString()
  { return mappedPath+" -> "+resource;
  }
}
