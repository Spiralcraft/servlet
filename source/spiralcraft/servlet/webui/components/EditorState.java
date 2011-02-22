//
//Copyright (c) 1998,2009 Michael Toth
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
package spiralcraft.servlet.webui.components;

import java.net.URI;

import spiralcraft.command.Command;
import spiralcraft.data.session.Buffer;
import spiralcraft.lang.util.ChannelBuffer;
import spiralcraft.servlet.webui.ControlGroupState;


public class EditorState<T extends Buffer>
  extends ControlGroupState<T>
{
  
  private boolean redirect;
  private URI redirectURI;
  private Command<?,?,?> postSaveCommand;
  public final ChannelBuffer<?> trigger;
  
  public EditorState(EditorBase<T> editor,ChannelBuffer<?> trigger)
  { 
    super(editor);
    this.trigger=trigger;
  }
  
  public void setRedirect(boolean redirect)
  { this.redirect=redirect;
  }
    
  public boolean isRedirect()
  { return redirect;
  }
  
  public void setRedirectURI(URI redirectURI)
  { this.redirectURI=redirectURI;
  }
  
  public URI getRedirectURI()
  { return redirectURI;
  }
  
  public Command<?,?,?> getPostSaveCommand()
  { return postSaveCommand;
  }
  
  public void setPostSaveCommand(Command<?,?,?> postSaveCommand)
  { this.postSaveCommand=postSaveCommand;
  }

}