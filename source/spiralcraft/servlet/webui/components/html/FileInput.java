//
//Copyright (c) 1998,2008 Michael Toth
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
package spiralcraft.servlet.webui.components.html;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import spiralcraft.text.markup.MarkupException;

import spiralcraft.textgen.EventContext;
import spiralcraft.textgen.compiler.TglUnit;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;

import spiralcraft.lang.BindException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Binding;
import spiralcraft.log.ClassLog;

import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.text.html.URLEncoder;

public class FileInput
  extends Control<URI>
{
  private static final ClassLog log
    =ClassLog.getInstance(TextInput.class);
  
  private String name;
  private String contextRelativeRoot;  
  private Binding<URI> rootUriX;

  public class Tag
    extends AbstractTag
  {
    @Override
    protected String getTagName(EventContext context)
    { return "input";
    }

    @Override
    protected void renderAttributes(EventContext context)
      throws IOException
    {   
      ControlState<URI> state=getState(context);
      renderAttribute(context.getWriter(),"type","file");
      renderAttribute(context.getWriter(),"name",state.getVariableName());
      super.renderAttributes(context);
    }
    
    @Override
    protected boolean hasContent()
    { return false;
    }
    
  };
  
  private Tag tag=new Tag();
  private ErrorTag errorTag=new ErrorTag(tag);
  
  
  public void setName(String name)
  { this.name=name;
  }
  
  /**
   * <p>The context-relative path which serves as the root of the file
   *   repository.
   * </p>
   * 
   *  <p>This value will be resolved with respect to the servlet context
   *    root directory (root of the web application). Leading "/" are
   *    stripped.
   *  </p>
   * 
   * 
   * @param contextRelativeRoot
   * @Deprecated Use setRootUriX
   */
  public void setContextRelativeRoot(String contextRelativeRoot)
  { 
    while (contextRelativeRoot!=null && contextRelativeRoot.startsWith("/"))
    { contextRelativeRoot=contextRelativeRoot.substring(1);
    }
    this.contextRelativeRoot=contextRelativeRoot;
  }
  
  /**
   * The URI which represents the root of the repository.
   * @param rootUriX
   */
  public void setRootUriX(Binding<URI> rootUriX)
  { this.rootUriX=rootUriX;
  }
  
  public Tag getTag()
  { return tag;
  }

  public ErrorTag getErrorTag()
  { return errorTag;
  }


  @Override
  @SuppressWarnings("unchecked") // Not using generic versions
  public void bind(List<TglUnit> childUnits)
    throws BindException,MarkupException
  { 
    super.bind(childUnits);
    if (rootUriX!=null)
    { rootUriX.bind(getFocus());
    }
    Form form=findElement(Form.class);
    if (form!=null)
    { form.setMimeEncoded(true);
    }
    

    if (target==null)
    { log.fine("Not bound to anything (formvar name="+name+")");
    }
  }
  
  @Override
  public String getVariableName()
  { return name;
  }
  
  @Override
  public ControlState<URI> createState()
  { return new ControlState<URI>(this);
  }
  
//  @Override
//  public ControlState<URI> createState()
//  { return new ControlState<URI>(this);
//  }
  
  @Override
  public void render(EventContext context)
    throws IOException
  { 
    if ( getState(context).isErrorState())
    { errorTag.render(context);
    }
    else
    { tag.render(context);
    }
    super.render(context);
  }
  

  @Override
  public void gather(ServiceContext context)
  {
    if (debug)
    { log.fine(context.getRequest().getContentType());
    }
    ControlState<URI> state=getState(context);
   
    // Only update if changed
    if (context.getPost()!=null)
    {
      String filename
        =context.getPost().getOne(state.getVariableName()+".filename");
      
      if (filename!=null && target!=null)
      {
        String temporaryURI
          =context.getPost().getOne(state.getVariableName()+".temporaryURI");
        
        try
        {
          if (debug)
          {
            log.fine("Got file "+filename);
            log.fine("Got uri "+temporaryURI);
          }
          Resource tempResource=Resolver.getInstance().resolve(temporaryURI);
          
          URI rootURI=null;
          
          if (rootUriX!=null)
          { rootURI=rootUriX.get();
          }
          else
          { rootURI=context.getContextRoot().getURI();
          }
          
          if (contextRelativeRoot!=null)
          { rootURI=rootURI.resolve(contextRelativeRoot);
          }
            
          Resource rootResource
            =Resolver.getInstance().resolve(rootURI);
          
          if (rootResource==null)
          { 
            throw new IllegalArgumentException
              ("Resource "+rootURI+" not found");
          }

          Container rootContainer=rootResource.asContainer();
          
          if (rootContainer==null)
          { 
            throw new IllegalArgumentException
              (rootResource.getURI()+" is not a directory");
          }
          
          Resource targetResource=rootContainer.getChild(filename);
          
          while (targetResource.exists())
          {
            filename=nextUniqueName(filename);
            targetResource=rootContainer.getChild(filename);
          }

          targetResource.copyFrom(tempResource);
          
          // Set the filename
          // XXX Incorporate dynamic path element as well
          URI fileURI=URI.create(URLEncoder.encode(filename));
          

          state.setValue(fileURI);
          if (!target.set(fileURI))
          { log.fine("target.set() returned false: "+target+" with "+fileURI);
          }
          else
          { state.valueUpdated();
          }
                    
        }
        catch (IOException x)
        { handleException(context,x);
        }
        catch (AccessException x)
        { handleException(context,x);
        }
        catch (NumberFormatException x)
        { handleException(context,x);
        }
        catch (IllegalArgumentException x)
        { handleException(context,x);
        }

      }
    }

  }
  

  private String nextUniqueName(String filename)
  {
    int dotPos=filename.indexOf('.');
    String prefix=(dotPos>0)?filename.substring(0,dotPos):filename;
    String suffix=(dotPos>0)?filename.substring(dotPos):"";
    
    int num=2;
    if (prefix.endsWith(")"))
    {
      int parenPos=prefix.lastIndexOf("(");
      if (parenPos>-1)
      { 
        String numString=prefix.substring(parenPos+1,prefix.length()-1);
        try
        { 
          num=Integer.parseInt(numString)+1;
          prefix=prefix.substring(0,parenPos).trim();
        }
        catch (NumberFormatException x)
        { // Ignore, last paren does not contain a number
        }
      }
    }
    prefix+=" ("+num+")";
    return prefix+suffix;
  }
  
  @Override
  public void scatter(ServiceContext context)
  {
    ControlState<URI> state=getState(context);
    if (target!=null)
    {
      try
      {
        URI val=target.get();
        state.updateValue(val);
      }
      catch (AccessException x)
      { handleException(context,x);
      }
      
    }
  }

  
  @Override
  public void bindSelf()
    throws BindException
  { 
    tag.bind(getFocus());
    errorTag.bind(getFocus());
  }    

}

