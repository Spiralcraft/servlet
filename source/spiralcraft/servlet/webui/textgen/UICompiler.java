package spiralcraft.servlet.webui.textgen;

import spiralcraft.textgen.compiler.TglCompiler;
import spiralcraft.textgen.compiler.TglUnit;

import spiralcraft.vfs.Resource;


public class UICompiler
  extends TglCompiler<RootUnit>
{
  @Override
  protected UICompiler clone()
  { return new UICompiler();
  }
  
  @Override
  protected RootUnit createDocletUnit(TglUnit parent,Resource resource)
  { return new RootUnit(parent,resource,this);
  }
}
