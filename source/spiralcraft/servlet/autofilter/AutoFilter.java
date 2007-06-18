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

import spiralcraft.util.Path;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;


/**
 * <P>A Filter which applies to a web server resource context, that 
 *   is configured as a Bean and read from a spiralcraft.data XML file
 *   contained in a directory in the resource context.
 *   
 * <P>Multiple AutoFilters can be in effect for a given resource context.
 *   A server may have a default Filter, a Filter for a specific domain,
 *   and a Filter for each resource path within the domain. Each Filter
 *   will back-reference a Filter of the same type configured at a more
 *   general location (a parent directory).
 *   
 * <P>The Controller will construct a FilterChain for a given request which
 *   chains together all relevant Filters for a given path.
 */
public abstract class AutoFilter
  implements Filter
{
  private boolean additive;
  private boolean overridable;
  private boolean global;
  private Path path;
  protected String pattern;
  
  protected FilterConfig config;

  /**
   * @return whether this Filter augments a more general Filter instance
   *   for an enclosing scope. If false, this Filter will override
   *   the general Filter.
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
   */
  public boolean isOverridable()
  { return overridable;
  }

  /**
   * Indicate whether this Filter can be overridden in a more
   *   specific scope.
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
  
  public void setPath(Path path)
  { 
    System.err.println("AutoFilter.setPath(): "+path.format("/"));
    
    this.path=path;
  }
  
  /**
   * 
   * @return The pattern which determines whether this Filter applies to a given path. The
   *   pattern will be matched against the path.
   */
  public void setPattern(String pattern)
  { this.pattern=pattern;
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
    System.err.println("AutoFilter.appliesToPath(): "+pattern+"->"+relativePath.format("/"));
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
   * Called when a more general Filter already applies that
   *   this filter adds to or overrides.
   */
  public abstract void setParentInstance(AutoFilter parentInstance);
  
  /**
   * @return A unique identifier for the type of filter 
   */
  public abstract String getFilterType();
  
  
  public void init(FilterConfig config)
  { 
    this.config=config;
    if (pattern==null)
    { pattern="*";
    }
  }
  
  public abstract void doFilter
    (ServletRequest request
    ,ServletResponse response
    ,FilterChain chain
    )
    throws IOException,ServletException;
  
  public void destroy()
  { }
  
}
