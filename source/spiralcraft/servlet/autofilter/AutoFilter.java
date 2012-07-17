//
//Copyright (c) 1998,2007 Michael Toth
//Spiralcraft Inc., All Rights Reserved
//
//This package is part of the Spiralcraft project and is licensed under
//a multiple-license framework.
//
//You may not use this file except in compliance with the terms found in the
//SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
//at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
//Unless otherwise agreed to in writing, this software is distributed on an
//"AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.servlet.autofilter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import spiralcraft.common.declare.Declarable;
import spiralcraft.common.declare.DeclarationInfo;
import spiralcraft.servlet.kit.AbstractFilter;
import spiralcraft.util.Path;
import spiralcraft.vfs.Resource;


/**
 * <p>A Filter which participates in a system of filters configured within
 *   and mapped to individual directories within a servlet context, that 
 *   is configured as a Bean (directly, from a spiralcraft.data XML file in
 *   the target directory, from a spiralcraft.builder AssemblyClass, or 
 *   via automatic configuration from initialization parameters).
 * </p>
 *  
 * <p>Multiple AutoFilters can be in effect for a given resource context.
 *   A server may have a default Filter, a Filter for a specific domain,
 *   and a Filter for each resource path within the domain. Each Filter
 *   will back-reference a Filter of the same type configured at a more
 *   general location (a parent directory).
 * </p>
 *   
 * <p>Many AutoFilters can be used standalone (ie. traditional web.xml or
 *   container level configuration) or via the Controller mechanism which
 *   integrates .control.xml files in the directory tree under the context.
 * </p>
 * 
 * <p>When used with the Controller, the Controller will construct a
 *   FilterChain for a given request which chains together all relevant 
 *   Filters for a given path.
 * </p>
 */
public abstract class AutoFilter
  extends AbstractFilter
  implements Declarable
{
  private static int ID=0;
  private boolean additive=true;
  private boolean overridable=true;
  private boolean global;
  private Path path;
  private String sessionAttributeName
    ="autoFilter."+Integer.toString(ID++)+".session";
  
  protected boolean debug;
  protected String pattern;
  protected AutoFilter parent;
  protected AutoFilter generalInstance;
  protected Resource container;
  protected DeclarationInfo declarationInfo;

  
  /**
   * @return whether this Filter augments a more general Filter instance
   *   for an enclosing scope. If false, this Filter will override
   *   the general Filter.
   *   
   * <P>Generally, this defaults to true, but some specific types of filters
   *   might want to disable the influence of any parent filter by setting 
   *   this to false.
   */
  public boolean isAdditive()
  { return additive;
  }
  
  /**
   * Indicate whether this Filter adds to a more general Filter 
   *   for an enclosing scope.
   */
  public void setAdditive(boolean additive)
  { this.additive=additive;
  }

  /**
   * @return whether this Filter can be overridden in a more 
   *   specific scope.
   *   
   * <P>Generally, this defaults to true, but some specific types of filters
   *   (ie. security related) might not permit overrides.
   */
  public boolean isOverridable()
  { return overridable;
  }

  /**
   * <P>Indicate whether this Filter can be overridden in a more
   *   specific scope.
   *   
   * <P>Generally, this defaults to true, but some specific types of filters
   *   (ie. security related) might not permit overrides.
   */
  public void setOverridable(boolean overridable)
  { this.overridable=overridable;
  }
  
  /**
   * @return whether this Filter applies to all the subdirectories
   *   of the directory where it is declared.
   */
  public boolean isGlobal()
  { return global;
  }
  

  /**
   * Indicate whether this Filter applies to subdirectories of the
   *   directory where it is declared.
   */
  public void setGlobal(boolean global)
  { this.global=global;
  }
  
  /**
   * 
   * @return The absolute URI path where this Filter is in scope. This
   *   is set by the controller.
   */
  public Path getPath()
  { return path;
  }
  
  protected Path getRelativePath(HttpServletRequest request)
  {
    Path path=Path.create(contextAdapter.getRelativePath(request));
    return getPath().relativize(path);
  }
    
  public void setPath(Path path)
  { 
//    System.err.println("AutoFilter.setPath(): "+path.format("/"));
    
    this.path=path;
  }
  
  public void setContainer(Resource container)
  { this.container=container;
  }
  
  /**
   * 
   * @return The resource container that holds the filter definitions for
   *   this part of the URI tree.
   *   
   */
  public Resource getContainer()
  { return container;
  }
  
  /**
   * <p>The pattern which determines whether this Filter applies to a 
   *   given path. The pattern will be matched against the path.
   * </p>
   */
  public void setPattern(String pattern)
  { this.pattern=pattern;
  }
  
  /**
   * 
   * @return The pattern which determines whether this Filter applies to a 
   *   given path. The pattern will be matched against the path.
   */
  public String getPattern()
  { return pattern;
  }
  
  /**
   * @return whether this Filter applies to the specified resource
   *   path. The path is absolute, but it will always start with 
   *   the result of getPath(), which can be subtracted from the
   *   supplied path if necessary for comparison.
   */
  public boolean appliesToPath(Path path)
  {
    Path relativePath=path.subPath(getPath().size());
//    System.err.println("AutoFilter.appliesToPath(): "+pattern+"->"+relativePath.format("/"));
    if (this.isGlobal())
    {
      if (pattern.equals("*"))
      { return true;
      }
      else if (pattern.startsWith("*"))
      {
        if (relativePath.lastElement()!=null
            && relativePath.lastElement().endsWith(pattern.substring(1))
            )
        { return true;
        }
        else
        { return false;
        }
      }
      else
      {
        if (relativePath.lastElement()!=null && relativePath.lastElement().equals(pattern))
        { return true;
        }
      }
    }
    else
    { 
      if (pattern.equals("/"))
      {
//        System.err.println("AutoFilter: relativePath="+relativePath);
        if (relativePath.size()==0)
        { 
          // Only applies to the directory
          return true;
        }
        else
        { return false;
        }
      }
      else if (pattern.equals("*"))
      { return true;
      }
      else if (pattern.startsWith("*"))
      {
        if (relativePath.lastElement()!=null
           && relativePath.lastElement().endsWith(pattern.substring(1))
           )
        { return true;
        }
        else
        { return false;
        }
      }
      else 
      {
        if (relativePath.size()>0 
            && relativePath.getElement(0)!=null
            && relativePath.getElement(0).equals(pattern)
            )
        { return true;
        }
      }
    }
    return false;
  }
  
  /**
   * Called when a more general Filter of the same common type already applies
   *   that this filter adds to or overrides. 
   *   
   */
  public void setGeneralInstance(AutoFilter parentInstance)
  { this.generalInstance=parentInstance;
  }
  
  public Class<? extends AutoFilter> getCommonType()
  { return getClass();
  }

  /**
   * 
   * @param parent The parent filter in the chain. 
   */
  public void setParent(AutoFilter parent)
  { this.parent=parent;
  }
  
  @Override
  public void init(FilterConfig config)
    throws ServletException
  { 
    super.init(config);
    if (pattern==null)
    { pattern="*";
    }
  }
  
  @Override
  public abstract void doFilter
    (ServletRequest request
    ,ServletResponse response
    ,FilterChain chain
    )
    throws IOException,ServletException;
  
  @Override
  public void destroy()
  { }
  
  
  /**
   * Find a Filter among this Filters containers
   * 
   * @param <X>
   * @param clazz
   * @return The Element with the specific class or interface, or null if
   *   none was found
   */
  @SuppressWarnings("unchecked") // Downcast from runtime check
  public <X extends Filter> X findFilter(Class<X> clazz)
  {
    if (clazz.isAssignableFrom(getClass()))
    { return (X) this;
    }
    else if (parent!=null)
    { return parent.<X>findFilter(clazz);
    }
    else
    { return null;
    }
  }
  
  public void sendError(ServletResponse servletResponse,Throwable x)
    throws IOException
  {
    HttpServletResponse response=(HttpServletResponse) servletResponse;
    response.setStatus(501);
  
    PrintWriter printWriter=new PrintWriter(response.getWriter());
    printWriter.write(x.toString());

    x.printStackTrace(printWriter);
    printWriter.flush();
  }
  
  /**
   * Turn on debugging output
   * @param debug
   */
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  
  protected Object newPrivateSessionState(HttpServletRequest request)
  { return new Object();
  }
  
  protected final void setPrivateSessionState
    (HttpServletRequest request,Object privateSessionState)
  { 
    HttpSession session=request.getSession(true);
    session.setAttribute(sessionAttributeName,privateSessionState);    
  }
  
  @SuppressWarnings("unchecked")
  protected final <X> X getPrivateSessionState
    (HttpServletRequest request,boolean create)
  { 
    HttpSession session=request.getSession(create);
    if (session!=null)
    { 
      Object privateSession=session.getAttribute(sessionAttributeName);
      if (privateSession==null && create)
      { 
        privateSession=newPrivateSessionState(request);
        session.setAttribute(sessionAttributeName,privateSession);
      }
      return (X) privateSession;
    }
    return null;
  }
  
  @Override
  public void setDeclarationInfo(
    DeclarationInfo declarationInfo)
  { this.declarationInfo=declarationInfo;
  }

  @Override
  public DeclarationInfo getDeclarationInfo()
  { return declarationInfo;
  }  
}
  

