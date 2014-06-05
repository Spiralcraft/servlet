//
//Copyright (c) 2011 Michael Toth
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
import java.util.HashSet;
import java.util.List;

import spiralcraft.app.Dispatcher;
import spiralcraft.app.Message;
import spiralcraft.app.MessageHandlerChain;
import spiralcraft.app.Scaffold;
import spiralcraft.app.kit.AbstractMessageHandler;
import spiralcraft.app.kit.DynamicHandler;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.text.MessageFormat;
import spiralcraft.textgen.OutputContext;
import spiralcraft.textgen.PrepareMessage;
import spiralcraft.textgen.RenderMessage;
import spiralcraft.textgen.compiler.TglUnit;
import spiralcraft.util.tree.Order;


/**
 * <p>Renders the outer structure of an HTML page
 * </p>
 * 
 * @author mike
 *
 */
public class Page
  extends ControlGroup<Void>
{
  public class Doctype extends AbstractMessageHandler
  {
    { 
      type=RenderMessage.TYPE;
    }

    @Override
    protected void doHandler(
      Dispatcher dispatcher,
      Message message,
      MessageHandlerChain next)
    {
      try
      {
        Appendable out=OutputContext.get();
        if (contentType!=null && contentType.equals("application/xhtml+xml"))
        { 
          out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
          out.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\"");
          out.append(" ");
          out.append("\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\r\n");

        }
        else
        { out.append("<!DOCTYPE html>");
        }
        next.handleMessage(dispatcher,message);
      }
      catch (IOException x)
      { throw new RuntimeException(x);
      }
          
    }
    
    
  }

  public class HeaderEpilogue extends AbstractMessageHandler
  {
    @Override
    protected void doHandler(
      Dispatcher dispatcher,
      Message message,
      MessageHandlerChain next)
    {
      try
      {
        if (message.getType()==PrepareMessage.TYPE)
        { ((PageState) getState()).headerEpilogue.setLength(0);
        }
        else if (message.getType()==RenderMessage.TYPE)
        { OutputContext.get().append(((PageState) getState()).headerEpilogue);
        }
        next.handleMessage(dispatcher,message);
      }
      catch (IOException x)
      { throw new RuntimeException(x);
      }
          
    }
  }

  public class BodyPrologue extends AbstractMessageHandler
  {
    @Override
    protected void doHandler(
      Dispatcher dispatcher,
      Message message,
      MessageHandlerChain next)
    {
      try
      {
        if (message.getType()==PrepareMessage.TYPE)
        { ((PageState) getState()).bodyPrologue.setLength(0);
        }
        else if (message.getType()==RenderMessage.TYPE)
        { OutputContext.get().append(((PageState) getState()).bodyPrologue);
        }
        next.handleMessage(dispatcher,message);
      }
      catch (IOException x)
      { throw new RuntimeException(x);
      }
          
    }
  }
  
  public class Footer extends AbstractMessageHandler
  {
    @Override
    protected void doHandler(
      Dispatcher dispatcher,
      Message message,
      MessageHandlerChain next)
    {
      try
      {
        if (message.getType()==PrepareMessage.TYPE)
        { ((PageState) getState()).footer.setLength(0);
        }
        next.handleMessage(dispatcher,message);
        if (message.getType()==RenderMessage.TYPE)
        { OutputContext.get().append(((PageState) getState()).footer);
        }
      }
      catch (IOException x)
      { throw new RuntimeException(x);
      }
          
    }
  }

  public class Tag extends AbstractTag
  {
    { 
      addNewLine=true;
      standardClasses=null;
    }
    
    @Override
    protected String getTagName(Dispatcher dispatcher)
    { return "html";
    }

    @Override
    protected boolean hasContent()
    { return true;
    }
    
  }
  
  public class Head
    extends AbstractTag
  {

    { 
      addNewLine=true;
      standardClasses=null;
    }
    
    @Override
    protected String getTagName(
      Dispatcher context)
    { return "head";
    }

    @Override
    protected boolean hasContent()
    { return true;
    }
  }

  public class Body
    extends AbstractTag
  {

    { addNewLine=true;
    }

    @Override
    protected String getTagName(
      Dispatcher context)
    { return "body";
    }

    @Override
    protected boolean hasContent()
    { return true;
    }
  }
  
  public class Title
    extends AbstractTag
  {

    { 
      tagPosition=-1;
      addNewLine=true;
      standardClasses=null;
    }

    @Override
    protected String getTagName(
      Dispatcher context)
    { return "title";
    }

    @Override
    protected boolean hasContent()
    { return true;
    }
    
    @Override
    public void renderContent
      (Dispatcher dispatcher,Message message,MessageHandlerChain next)
      throws IOException
    { 
      if (title!=null)
      { title.render(OutputContext.get());
      }
      super.renderContent(dispatcher,message,next);
    }
  }  
  
  /**
   * <p>Specify a list of CSS URIs to load in the page head
   * </p>
   * @param links
   */
  public void setCss(URI[] links)
  { 
    for (URI link:links)
    { requireCSS(link);
    }
  }
  
  /**  
   * <p>Specify a list of javascript URIs to load in the page head
   * </p>
   */
  public void setScripts(URI[] scripts)
  {
    for (URI script:scripts)
    { requireScript(script);
    }
  }

  private HashSet<URI> links=new HashSet<URI>();
  private HashSet<URI> scripts=new HashSet<URI>();
  
  
  private Doctype doctype=new Doctype();
  private Tag tag=new Tag();
  private DynamicHandler headChain=new DynamicHandler();
  
  private Body body=new Body();
  
  private DynamicHandler linkHandler=new DynamicHandler();
  private DynamicHandler scriptHandler=new DynamicHandler();
  private MessageFormat title;
  private URI cssBase;
  private URI jsBase;
  
  @Override
  protected void addHandlers() 
    throws ContextualException
  { 
    super.addHandlers();
    addHandler(doctype);
    addHandler(tag);
    addHandler(headChain);
    headChain.addHandler(new Head());
    headChain.addHandler(new Title());
    headChain.addHandler(linkHandler);
    headChain.addHandler(scriptHandler);
    headChain.addHandler(new HeaderEpilogue());
    headChain.setOrder(Order.PRE);
    addHandler(body);
    addHandler(new BodyPrologue());
    addHandler(new Footer());
  }
  
  public Tag getTag()
  { return tag;
  }
  
  public Body getBodyTag()
  { return body;
  }
  
  public void setCssBase(URI cssBase)
  { this.cssBase=cssBase;
  }
  
  public URI getCssBase()
  { return cssBase;
  }
  
  public void setJsBase(URI jsBase)
  { this.jsBase=jsBase;
  }

  public URI getJsBase()
  { return jsBase;
  }
  
  public void addTagToHead(AbstractTag tag)
  { 
    tag.setTagPosition(-1);
    headChain.addHandler(tag);
  }
  
  public void addTagToBody(AbstractTag tag)
  { addHandler(tag);
  }
  
  
  public void requireCSS(URI css)
  { 
    if (links.add(css))
    {
      LinkTag link=new LinkTag();
      link.setTagPosition(-1);
      link.setRel("stylesheet");
      link.setType("text/css");
      link.setHREF(MessageFormat.create(css.toString()));
      linkHandler.addHandler(link);
    }
  }
  
  
  public void requireScript(URI uri)
  {
    if (scripts.add(uri))
    {
      ScriptTag script=new ScriptTag();
      script.setTagPosition(-1);
      script.setType("text/javascript");
      script.setSrc(uri.toString());
      scriptHandler.addHandler(script);
    }
  }
  
  public void setTitle(MessageFormat title)
  { 
    removeSelfContextual(this.title);
    this.title=title;
    addSelfContextual(this.title);
  }
  
  public void setBodyOnLoad(String script)
  { body.appendAttribute("onload",script);
  }
  
  @Override
  protected void bindComplete(Focus<?> focus)
    throws ContextualException
  {
    headChain.completeBind();
    linkHandler.completeBind();
    scriptHandler.completeBind();
    super.bindComplete(focus);
  }
  
  @Override
  protected List<Scaffold<?>>
    expandChildren(Focus<?> focus,List<TglUnit> childUnits)
    throws ContextualException
  {
    List<Scaffold<?>> list=super.expandChildren(focus,childUnits);
    
    return list;
    
  }

  @Override
  public PageState createState()
  { return new PageState(this);
  }
  
  public PageState getState()
  { return (PageState) super.getState();
  }
  
  public Appendable getHead()
  { return getState().headerEpilogue;
  }

  public Appendable getStartOfBody()
  { return getState().bodyPrologue;
  }

  public Appendable getEndOfBody()
  { return getState().footer;
  }
}

class PageState
  extends ControlGroupState<Void>
{

  StringBuffer headerEpilogue=new StringBuffer();
  StringBuffer footer=new StringBuffer();
  StringBuffer bodyPrologue=new StringBuffer();
  
  public PageState(Page page)
  { super(page);
  }
}