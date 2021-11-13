package spiralcraft.servlet.autofilter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.autofilter.spi.FocusFilter;

public class ExpressionFilter
  extends AutoFilter
{
  { 
    setGlobal(true);
    setPattern("*");
  }
  
  private boolean bound;
  private Binding<?> pre;
  private Binding<?> post;
  private Binding<Boolean> guard;
  
  private void bind(HttpServletRequest request) 
    throws BindException
  { 
    Focus<?> focus=FocusFilter.getFocusChain(request);
    if (pre!=null)
    { pre.bind(focus);
    }
    if (guard!=null)
    { guard.bind(focus);
    }
    if (post!=null)
    { post.bind(focus);
    }
  }

  public void setPre(Binding<?> x)
  { this.pre=x;
  }

  public void setGuard(Binding<Boolean> x)
  { this.guard=x;
  }
  
  public void setPost(Binding<?> x)
  { this.post=x;
  }

  @Override
  public void doFilter(
    ServletRequest request,
    ServletResponse response,
    FilterChain chain)
    throws IOException,
    ServletException
  {
    if (!bound)
    { 
      try
      {
        bind((HttpServletRequest) request);
        bound=true;
      }
      catch (BindException x)
      { 
        throw new ServletException
          ("Error binding expression "+getDeclarationInfo(),x);
      }
    }
    if (pre!=null)
    { pre.get();
    }
    if (guard==null || Boolean.TRUE.equals(guard.get()))
    { chain.doFilter(request, response);
    }
    if (post!=null)
    { post.get();
    }
    
  }

}
