package spiralcraft.servlet.rpc;

import spiralcraft.lang.Contextual;

public interface Handler
  extends Contextual
{
  
  /**
   * Configure the name of the handler, which is the value of the path segment
   *   that will cause this handler to be invoked.
   * 
   * @param name
   */
  public void setName(String name);
  
  /**
   * 
   * @return The name of the handler, which is the value of the path segment
   *   that will cause this handler to be invoked.
   */
  public String getName();
  
  /**
   * Handle the call
   */
  public abstract void handle(Call call);
}