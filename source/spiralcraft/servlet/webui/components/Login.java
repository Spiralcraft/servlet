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
package spiralcraft.servlet.webui.components;

import java.net.URI;

import javax.servlet.ServletException;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;


import spiralcraft.lang.AccessException;
import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.lang.spi.BeanReflector;
import spiralcraft.log.ClassLogger;

import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.LoginEntry;

import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;


/**
 * Provides common functionality for Editors
 * 
 * @author mike
 *
 */
public class Login
  extends ControlGroup<LoginEntry>
{
  private static final ClassLogger log
    =ClassLogger.getInstance(Login.class);

  private Channel<AuthSession> sessionChannel;
  private URI defaultURI;

  private Assignment<?>[] assignments;
  private Setter<?>[] setters;


  public void setDefaultURI(URI defaultURI)
  { this.defaultURI=defaultURI;
  }
  
  @SuppressWarnings("unchecked")
  protected void setupSession(Focus<?> parentFocus)
  {
    Focus<AuthSession> sessionFocus
      =(Focus<AuthSession>) parentFocus.findFocus(AuthSession.FOCUS_URI);
    if (sessionFocus!=null)
    { sessionChannel=sessionFocus.getSubject();
    }
  }

  public Command<LoginEntry,Void> loginCommand()
  {     
    return new QueuedCommand<LoginEntry,Void>
      (getState()
      ,new CommandAdapter<LoginEntry,Void>()
        { 
          public void run()
          { login();
          }
        }
      );
  }
  
  public LoginState createState()
  {
    return new LoginState(this);
  }

  private void login()
  {
    if (!sessionChannel.get().isAuthenticated())
    { 
      getState().setError
        ("Login failed, username/password combination not recognized");
    }
    else
    { getState().setValue(null);
    }
      
  }
 
  protected void handleInitialize(ServiceContext context)
  {
    super.handleInitialize(context);
    // Set up a LoginEntry?
  }
  
  protected void handlePrepare(ServiceContext context)
  { 
    
    LoginState state=(LoginState) context.getState();
    if (state.getValue()==null 
        && state.getReferer()==null
        )
    { 
      String refererParam=context.getQuery().getOne("referer");
      
      // Initial request- get referer for redirect
      URI refererURI;
      if (refererParam!=null)
      { refererURI=URI.create(refererParam);
      }
      else
      { 
        if (defaultURI!=null)
        { refererURI=defaultURI;
        }
        else
        { refererURI=URI.create("/");
        }
      }
      
      if (!refererURI.getPath().equals(context.getRequest().getRequestURI()))
      {
        state.setReferer
          (refererURI.toString());
        if (debug)
        { log.fine("Login referred from "+refererURI);
        }
      }
      else
      { 
        if (debug)
        { log.fine("Login referred from self! "+refererURI);
        }
        if (defaultURI!=null)
        { state.setReferer(defaultURI.toString());
        }
        else
        { state.setReferer("/");
        }
      }
    }
    super.handlePrepare(context);
    if (sessionChannel.get().isAuthenticated())
    { 
      if (setters!=null)
      {
        if (debug)
        { log.fine(toString()+": applying assignments on login");
        }
        for (Setter<?> setter: setters)
        {  setter.set();
        }
      }      
      try
      { context.redirect(URI.create(state.getReferer()));
      }
      catch (ServletException x)
      { state.setException(x);
      }
    }
    
  }
  
  /**
   * <p>Assignments get executed immediately after successful login
   * </p>
   * 
   * @param assignments
   */
  public void setAssignments(Assignment<?>[] assignments)
  { this.assignments=assignments;
  }  
  
  protected void newEntry()
  { 
    getState().setValue(new LoginEntry(sessionChannel));
  }
   

  protected Channel<?> bindTarget(Focus<?> parentFocus)
    throws BindException
  {
    // Only used to provide a reflector to ControlGroup, so it can make
    //   a ThreadLocalChannel for our LoginEntry, which is directly managed
    //   by this class.
    setupSession(parentFocus);
    return new AbstractChannel<LoginEntry>
      (BeanReflector.<LoginEntry>getInstance(LoginEntry.class))
        {
          @Override
          protected LoginEntry retrieve()
          { return null;
          }

          @Override
          protected boolean store(LoginEntry val) throws AccessException
          { return false;
          }
        };

  }
  
  @SuppressWarnings("unchecked")
  protected Focus<?> bindExports()
    throws BindException
  {
    setters=bindAssignments(assignments);
    return super.bindExports();
    
  }
  
  @Override
  protected void scatter(ServiceContext context)
  { 
    LoginEntry lastEntry=getState().getValue();
   
    super.scatter(context);
    if (getState().getValue()==null)
    { 
      if (lastEntry==null)
      { newEntry();
      }
      else
      { getState().setValue(lastEntry);
      }
    }
  } 
  
}

class LoginState
  extends ControlGroupState<LoginEntry>
{
  private String referer;
  
  public LoginState(Login login)
  { super(login);
  }
  
  public void setReferer(String referer)
  { this.referer=referer;
  }
  
  public String getReferer()
  { return referer;
  }
}

