package spiralcraft.servlet.webui.textgen;

import spiralcraft.textgen.ResourceUnit;

import spiralcraft.vfs.Resource;

import java.net.URI;

import java.io.IOException;

public class UIResourceUnit
  extends ResourceUnit<RootUnit>
{
  public UIResourceUnit(URI uri)
    throws IOException
  { super(uri);
  }

  public UIResourceUnit(Resource resource)
  { super(resource);
  }
  
  @Override
  public UICompiler createCompiler()
  { return new UICompiler();
  }
  
  @Override
  public RootUnit getUnit()
  { 
    return (RootUnit) super.getUnit();

  }
}
