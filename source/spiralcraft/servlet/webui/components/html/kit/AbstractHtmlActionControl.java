package spiralcraft.servlet.webui.components.html.kit;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.servlet.webui.components.AbstractActionControl;
import spiralcraft.servlet.webui.components.html.AbstractTag;
import spiralcraft.servlet.webui.components.html.ErrorTag;
import spiralcraft.servlet.webui.components.html.JSClient;
import spiralcraft.servlet.webui.components.html.PeerJSTag;
import spiralcraft.text.MessageFormat;

public abstract class AbstractHtmlActionControl<Tcontext,Tresult>
  extends AbstractActionControl<Tcontext,Tresult>
  implements ServerPeer
{
  protected String name;
  protected JSClient jsClient;
  protected PeerJSTag peerJSTag;
  protected AbstractTag tag;
  protected ErrorTag errorTag=new ErrorTag();
  private boolean anchor;
  private MessageFormat anchorId;
  

  public void setName(String name)
  { this.name=name;
  }
  
  @Override
  public String getVariableName()
  { return name;
  }
  
  /**
   * Cause the UI to scroll to this button when it is used to submit a form.
   * 
   * @param anchor
   */
  public void setAnchor(boolean anchor)
  { this.anchor=anchor;
  }
  
  public void setAnchorId(MessageFormat anchorId)
  { 
    this.anchor=true;
    this.anchorId=anchorId;
  }
  
  public String resolveAnchorId()
  { return anchorId!=null?anchorId.render():getState().getId();
  }
  
  public void setPeerJSTag(PeerJSTag scriptTag)
  { 
    if (jsClient==null)
    { jsClient=new JSClient();
    }
    if (scriptTag!=peerJSTag)
    { peerJSTag=scriptTag;
    }
  }
  
  public PeerJSTag getPeerJSTag()
  {
    if (peerJSTag==null)
    { 
      setPeerJSTag(new PeerJSTag());
      // Always render after element has closed
      peerJSTag.setTagPosition(1);
    }
    return peerJSTag;
  }
  
  public ErrorTag getErrorTag()
  { return errorTag;
  }  
  
  @Override
  protected Focus<?> bindSelfFocus(Focus<?> focus) 
    throws ContextualException
  { 
    if (anchorId!=null)
    { addSelfContextual(anchorId);
    }
    return super.bindSelfFocus(focus);
  }
  
  @Override
  public String getCSID()
  { return getState().getId();
  }
  
  @Override
  protected void addHandlers() 
    throws ContextualException
  {
    if (anchor)
    { 
      getPeerJSTag().addOnClickJS
        (new MessageFormat
          ("SPIRALCRAFT.dom.makeFormAnchor("
            +"this,"
            +"'#{|[:class:/spiralcraft.servlet/webui/components/html/kit/AbstractHtmlActionControl]"
              +".resolveAnchorId()|}'"
          +"); return true;"
          )
        );
      tag.setGenerateId(true);
    }
    
    if (peerJSTag!=null)
    { addHandler(peerJSTag);    
    }
    addHandler(errorTag);
    addHandler(tag);    
    if (jsClient!=null)
    { this.addSelfContextual(jsClient);
    }
    super.addHandlers();
  }    
}
