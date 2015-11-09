package spiralcraft.servlet.rpc;


public class Call
{
  public final Response response=new Response();
  public final Request request=new Request();
  
  public void respond(int statusCode,String message)
  { 
    response.setText(message);
    response.setStatus(statusCode);
  }
  
}
