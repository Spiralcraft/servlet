package spiralcraft.servlet.webui.textgen;

import spiralcraft.servlet.webui.UIComponent;
import spiralcraft.text.markup.MarkupException;
import spiralcraft.textgen.compiler.DocletUnit;
import spiralcraft.textgen.compiler.TglUnit;


import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;

import spiralcraft.vfs.Resource;

/**
 * The Root unit of a parsed textgen file
 * 
 * @author mike
 *
 */
public class UIUnit
  extends DocletUnit
{
  public UIUnit(TglUnit parent,Resource resource)
  {
    super(parent,resource);
  }
  
  @Override
  public UIComponent bind(Focus<?> focus)
    throws MarkupException
  {
    UIComponent element=new UIComponent(focus);
    
    try
    { element.bind(children);
    }
    catch (BindException x)
    { throw new MarkupException(x.toString(),getPosition());
    }
    return element;    
  }
  
}
