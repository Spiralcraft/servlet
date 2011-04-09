package spiralcraft.servlet.webui.textgen;

import spiralcraft.servlet.webui.RootComponent;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.compiler.DocletUnit;
import spiralcraft.textgen.compiler.TglUnit;


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
    throws MarkupException
  { return (RootComponent) bind(focus,null,new RootComponent());
  }
  
}
