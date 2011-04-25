package spiralcraft.servlet.webui.textgen;

import spiralcraft.servlet.webui.RootComponent;
import spiralcraft.textgen.compiler.DocletUnit;
import spiralcraft.textgen.compiler.TglUnit;


import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;

import spiralcraft.vfs.Resource;

/**
 * The Root unit of a parsed textgen file
 * 
 * @author mike
 *
 */
public class RootUnit
  extends DocletUnit
{
  public RootUnit(TglUnit parent,Resource resource)
  {
    super(parent,resource);
  }
  
  public RootComponent bindRoot(Focus<?> focus)
    throws ContextualException
  { return (RootComponent) bind(focus,null,new RootComponent());
  }
  
}
