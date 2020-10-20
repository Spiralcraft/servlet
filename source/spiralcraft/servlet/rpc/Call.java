package spiralcraft.servlet.rpc;

import spiralcraft.util.Path;

public interface Call
{

  
  /**
   * The remaining part of the path excluding the path segment being addressed
   */
  public String getPathInfo();

  /**
   * 
   * @return A Path that begins with the segment being addressed and contains
   *   the remaining path segments.
   */
  public Path getNextPath();
  
  public void respond(int statusCode,String message);
  
  public Request getRequest();
}
