//
//Copyright (c) 1998,2015 Michael Toth
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
package spiralcraft.servlet.webui.components.kit;


import java.io.IOException;
import java.net.URI;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.kit.Callable;
import spiralcraft.log.ClassLog;
import spiralcraft.servlet.webui.Control;
import spiralcraft.servlet.webui.ControlState;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.servlet.webui.components.html.Form;
import spiralcraft.text.MessageFormat;
import spiralcraft.text.ParseException;
import spiralcraft.text.html.URLEncoder;
import spiralcraft.util.Path;
import spiralcraft.util.string.StringUtil;
import spiralcraft.vfs.Container;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;

/**
 * Accepts a file upload and records the relative URI of the uploaded file
 * 
 * @author mike
 *
 */
public abstract class AbstractFileControl
  extends Control<URI>
{
  private static final URI CONTEXT_ROOT=URI.create("context:/");
  
  private static final ClassLog log
    =ClassLog.getInstance(AbstractFileControl.class);
  
  private String contextRelativeRoot;  
  private Binding<URI> rootUriX;
  private Binding<URI> dirUriX;
  private Binding<String> filenameX;
  private boolean createDirs;
  private boolean overwrite;
  private long maxSize;
  private boolean sanitizeInput;
  private Binding<?> onInput;
  private Callable<URI,?> onInputFn;
  private String name;
  

  private MessageFormat fileTooLargeError;
  { 
    try
    {
      fileTooLargeError
        =new MessageFormat("Input cannot be larger than {|.maxSize|} bytes");
      addSelfContextual(fileTooLargeError);
    }
    catch (ParseException x)
    { throw new RuntimeException(x);
    }
  }
  
  /**
   * Specify the suffix that will be used to generate the HTML control name
   * 
   * @param name
   */
  public void setName(String name)
  { this.name=name;
  }

  @Override
  public String getVariableName()
  { return name;
  }
  

  /**
   * An expression to be evaluated against the input value when input is 
   *   received.
   * 
   * @param onInput
   */
  public void setOnInput(Binding<?> onInput)
  { this.onInput=onInput;
  }
  
  /**
   * Convert the filename supplied by the client a string consisting only
   *   of letters, numbers, '.' and '_' for portability across filesystems.
   * 
   * @param sanitizeInput
   */
  public void setSanitizeInput(boolean sanitizeInput)
  { this.sanitizeInput=sanitizeInput;
  }
  
  
  public void setFileTooLargeError(MessageFormat fileTooLargeError)
  { 
    removeSelfContextual(this.fileTooLargeError);
    this.fileTooLargeError=fileTooLargeError;
    if (fileTooLargeError!=null)
    { addSelfContextual(fileTooLargeError);
    }
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
  
  /**
   * The subdirectory into which the file will be saved, specified as a 
   *   URI that is relative to the rootURI.
   * 
   * @param dirUriX
   */
  public void setDirUriX(Binding<URI> dirUriX)
  { this.dirUriX=dirUriX;
  }
  
  /**
   * The filename to use for the destination, without the 
   *   extension. The extension provided by the incoming file will be
   *   converted to lower case and appended to the result.
   * 
   * @param filenameX
   */
  public void setFilename(String filename)
  { 
    this.filenameX
      =new Binding<String>(Expression.<String>create("\""+filename+"\""));
  } 
  
  /**
   * The expression which provides the filename to use for the destination
   *   without the  extension. The extension provided by the incoming file will 
   *   be converted to lower case and appended to the result.
   * 
   * @param filenameX
   */
  public void setFilenameX(Binding<String> filenameX)
  { this.filenameX=filenameX;
  }
  
  /**
   * The maximum permitted size of the file, in bytes
   * 
   * @param maxSize
   */
  public void setMaxSize(long maxSize)
  { this.maxSize=maxSize;
  }
  
  /**
   * The maximum permitted size of the file, in bytes
   * 
   * @return maxSize
   */
  public long getMaxSize()
  { return maxSize;
  }
  
  /**
   * Whether to overwrite the destination file if it exists. Otherwise,
   *   a new file will be created with a unique extension added to the
   *   name part of the filename
   * 
   * @param overwrite
   */
  public void setOverwrite(boolean overwrite)
  { this.overwrite=overwrite;
  }
  
  /**
   * 
   * Auto-create directories specified in the dirUri property
   */
  public void setCreateDirs(boolean createDirs)
  { this.createDirs=createDirs;
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Focus<?> bindSelf(Focus<?> focus)
    throws ContextualException
  { 
    if (target!=null && !target.getContentType().isAssignableFrom(URI.class))
    { 
      throw new BindException
        ("FileInput invalid target type '"+target.getReflector().getTypeURI()+"' "
        +": cannot be assigned a file path of type class:/java/net/URI"
        );
    }
    
    focus=super.bindSelf(focus);
    if (rootUriX!=null)
    { rootUriX.bind(focus);
    }
    if (dirUriX!=null)
    { dirUriX.bind(focus);
    }
    if (filenameX!=null)
    { filenameX.bind(focus);
    }
    
    Form<?> form=findComponent(Form.class);
    if (form!=null)
    { form.setMimeEncoded(true);
    }

    if (target!=null && onInput!=null)
    { onInputFn=new Callable(getSelfFocus(),target.getReflector(),onInput);
    }
    
    if (target==null)
    { log.fine("Not bound to anything: "+getDeclarationInfo());
    }
    
    
    return focus;
  }

  @Override
  public ControlState<URI> createState()
  { return new ControlState<URI>(this);
  }

  private String encodeName(String name)
  {
    StringBuffer out=new StringBuffer();
    for (char chr:name.toCharArray())
    {
      if (chr=='.'
          || (chr<127 && Character.isLetterOrDigit(chr))
          )
      { out.append(chr);
      }
      else
      { out.append("_"+Integer.toHexString((int) chr));
      }
    }
    return out.toString();
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
        =context.getPost().getFirst(state.getVariableName()+".filename");
      
      if (sanitizeInput && filename!=null)
      { filename=encodeName(filename);
      }
      
      if (filenameX!=null && filename!=null)
      { 
        String extension=StringUtil.suffix(filename,'.');
        filename=filenameX.get();
        
        if (filename!=null && extension!=null && !extension.isEmpty())
        { filename=filename+'.'+extension;
        }
      }

      
      if (filename!=null && target!=null)
      {

        
        String temporaryURI
          =context.getPost().getFirst(state.getVariableName()+".temporaryURI");
        
        try
        {
          if (debug)
          {
            log.fine("Got file "+filename);
            log.fine("Got uri "+temporaryURI);
          }
          Resource tempResource=Resolver.getInstance().resolve(temporaryURI);
          
          if (maxSize>0 && tempResource.getSize()>maxSize)
          { 
            state.addError(StringUtil.renderToString(fileTooLargeError));
            return;
          }
          
          URI rootURI=null;
          
          if (rootUriX!=null)
          { rootURI=rootUriX.get();
          }
          else
          { rootURI=CONTEXT_ROOT;
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

          URI canonicalRootURI=rootResource.getURI();
          Container rootContainer=rootResource.asContainer();
          
          if (rootContainer==null)
          { 
            throw new IllegalArgumentException
              (rootResource.getURI()+" is not a directory");
          }
          
          
          Container dirContainer=rootContainer;
          
          if (dirUriX!=null)
          {
            URI dirURI=dirUriX.get();
            if (dirURI!=null)
            {
              if (dirURI.isAbsolute())
              {
                throw new IllegalArgumentException
                  (dirURI+" absolute URI not allowed");
              }
              
              Path path=new Path(dirURI.getPath(),'/');
              if (path.isAbsolute())
              { path=path.subPath(0);
              }
              
              if (createDirs)
              {
                for (String child:path)
                { dirContainer=dirContainer.ensureChildContainer(child);
                }
              }
              else
              {
                for (String child:path)
                { 
                  URI lastURI=dirContainer.getURI();
                  dirContainer=dirContainer.getChild(child).asContainer();
                  if (dirContainer==null)
                  { 
                    throw new IOException
                      ("Cannot find directory '"+child+"' in "+lastURI);
                  }
                }
                
              }
                
            }
          }
          

          URI relativePathURI
            =canonicalRootURI.relativize(dirContainer.getURI());
          
          Resource targetResource=dirContainer.getChild(filename);
          
          while (!overwrite && targetResource.exists())
          {
            filename=nextUniqueName(filename);
            targetResource=dirContainer.getChild(filename);
          }

          targetResource.copyFrom(tempResource);
          
          URI fileURI=relativePathURI.resolve(URLEncoder.encode(filename));
          

          state.setValue(fileURI);
          if (!target.set(fileURI))
          { log.fine("target.set() returned false: "+target+" with "+fileURI);
          }
          else
          { state.valueUpdated();
          }
                    
          if (onInputFn!=null)
          { onInputFn.evaluate(fileURI);
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

}