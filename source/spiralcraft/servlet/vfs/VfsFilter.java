package spiralcraft.servlet.vfs;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.servlet.autofilter.spi.FocusFilter;
import spiralcraft.vfs.context.FileSpace;

public class VfsFilter
  extends FocusFilter<FileSpace>
{

  private FileSpace fileSpace;
  
  public void setFileSpace(FileSpace fileSpace)
  { this.fileSpace=fileSpace;
  }
  
  @Override
  protected Focus<FileSpace> createFocus(
    Focus<?> parentFocus)
    throws BindException
  {
    fileSpace.bind(parentFocus);
    return parentFocus.chain(new SimpleChannel<FileSpace>(fileSpace,true));
  }

  @Override
  protected void popSubject(
    HttpServletRequest request)
  { fileSpace.pop();
  }

  @Override
  protected void pushSubject(
    HttpServletRequest request,
    HttpServletResponse response)
    throws BindException,
    ServletException
  { fileSpace.push();
  }

}
