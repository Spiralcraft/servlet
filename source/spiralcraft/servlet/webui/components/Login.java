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
import java.util.logging.Level;

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

import spiralcraft.servlet.autofilter.SecurityFilter;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.util.ArrayUtil;


/**
 * <p>Implements a Login control group- ie. a credential entry and login
 *   action which uses the spiralcraft.security.auth infrastructure.
 * </p>
 * 
 * <p>As a ControlGroup, exposes as its Focus subject a
 *   spiralcraft.security.auth.LoginEntry, which holds credential data.
 * </p>
 * 
 * <p>If not an 'in-place' login, will redirect back to a referring URI
 *   on success which may be passed in the request query. 
 * </p> 
 * 
 * 
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

  private Assignment<?>[] postAssignments;
  private Setter<?>[] postSetters;

  private Assignment<?>[] preAssignments;
  private Setter<?>[] preSetters;

  private boolean inPlace;
  private String failureMessage
    ="Login failed, username/password combination not recognized";
  
  
  private SecurityFilter securityFilter;
  
//  private boolean silent;

  /**
   * <p>Specify the URI to redirect to on successful login when no "referer" 
   *   parameter is provided,
   *   or when the "referer" parameter is invalid (ie. the login page itself,
   *   causing an redirect loop)
   * </p>
   * 
   * <p>If the defaultURI is not specified, it will be assumed to be the
   *   root of current web site- ie. "/"
   * </p>
   *   
   * @param defaultURI
   */
  public void setDefaultURI(URI defaultURI)
  { this.defaultURI=defaultURI;
  }
  

  /**
   * <p>Specify that the login form is an in-place login, and that no redirect
   *   should occur on success or failure.
   * </p>
   * 
   * @param inPlace
   */
  public void setInPlace(boolean inPlace)
  { this.inPlace=inPlace;
  }
  
  /**
   * <p>Specify the message that will be displayed when a login attempt fails
   * </p>
   * 
   * @param failureMessage
   */
  public void setFailureMessage(String failureMessage)
  { this.failureMessage=failureMessage;
  }
  
  protected void setupSession(Focus<?> parentFocus)
  {
    Focus<AuthSession> sessionFocus
      =parentFocus.<AuthSession>findFocus(AuthSession.FOCUS_URI);
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
          @Override
          public void run()
          { login(true);
          }
        }
      );
  }
  
  @Override
  public LoginState createState()
  {
    return new LoginState(this);
  }

  private void login(boolean interactive)
  {
    LoginState state=(LoginState) getState();
    if (debug)
    { 
      log.fine
        ("Attempting login "
        +(interactive?"(interactive)":"(non-interactive) for user=")
        +state.getValue().getUsername()
        );
    }
    if (preSetters!=null)
    {  
      if (debug)
      { log.fine(toString()+": applying pre-assignments before login");
      }
      for (Setter<?> setter: preSetters)
      {  setter.set();
      }
    }
    
    AuthSession session=sessionChannel.get();
      
    boolean authenticated;
    String username=null;;
    
    synchronized (session)
    {
      authenticated=session.authenticate();
      if (authenticated)
      { username=session.getPrincipal().getName(); 
      }
    }
      
    if (!authenticated)
    { 
      if (interactive)
      {
        state.addError
          (failureMessage);
        if (debug)
        { log.fine("Interactive login failure: entry="+getState().getValue());
        }
      }
      else
      { 
        if (debug)
        { log.fine("Non-interactive login failure: entry="+getState().getValue());
        }
      }
    }
    else
    { 
      if (debug)
      { 
        log.fine("Successful login for "
          +username
          );
      }

      if (securityFilter!=null && state.getValue().isPersistent())
      { securityFilter.writeLoginCookie(state.getValue());
      }
      

      if (postSetters!=null)
      {
        if (debug)
        { log.fine(toString()+": applying post assignments on login");
        }
        for (Setter<?> setter: postSetters)
        { setter.set();
        }
      }            

      state.setValue(null);
      newEntry();
    }
      
  }
 
  @Override
  protected void handleInitialize(ServiceContext context)
  {
    super.handleInitialize(context);
    // Set up a LoginEntry?
  }
  
  @Override
  protected void handlePrepare(ServiceContext context)
  { 
    
    LoginState state=(LoginState) context.getState();
    if (state.getValue()==null 
        && state.getReferer()==null
        )
    { 
      
      String refererParam
        =context.getQuery()!=null?context.getQuery().getOne("referer"):null;
      
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
    
    AuthSession session=sessionChannel.get();
    if (securityFilter!=null
        && !session.isAuthenticated()
        )
    {
      // Only process a login cookie if we are persisting logins
      if (debug)
      { log.fine("Not authenticated- checking cookie");
      }
      
      if (securityFilter.readLoginCookie(getState().getValue()))
      {
        if (debug)
        { log.fine("Logging in from cookie");
        }
        login(false);
      }
        
    }
    
    if (state.isErrorState())
    {
      if (debug)
      {
        log.log
          (Level.FINE
          ,"Error on login: "+ArrayUtil.format(state.getErrors(),",",null)
          ,state.getException()
          );
      }
    }
    else if (session.isAuthenticated())
    {  
      if (debug)
      { log.fine("Session is authenticated "+session);
      }
      if (!inPlace)
      {
        if (debug)
        { log.fine("Redirecting to "+state.getReferer());
        }
        try
        { context.redirect(URI.create(state.getReferer()));
        }
        catch (ServletException x)
        { state.setException(x);
        }
      }
    }
    else
    {
      if (debug)
      { log.fine("Session is not authenticated "+session);
      }
    }
    
  }


  

  
  
  
  /**
   * <p>Assignments which get executed prior to a login attempt (eg. to resolve
   *   credentials)
   * </p>
   * 
   * @param assignments
   */
  public void setPreAssignments(Assignment<?>[] assignments)
  { this.preAssignments=assignments;
  }  

  /**
   * <p>Assignments which get executed immediately after a successful login
   * </p>
   * 
   * <p>XXX refactor to setPostAssignments()
   * </p>
   * 
   * @param assignments
   */
  public void setAssignments(Assignment<?>[] assignments)
  { this.postAssignments=assignments;
  }  
  
  protected void newEntry()
  { 
    getState().setValue(new LoginEntry(sessionChannel));
  }
   

  @Override
  protected Channel<?> bindTarget(Focus<?> parentFocus)
    throws BindException
  {
    // Only used to provide a reflector to ControlGroup, so it can make
    //   a ThreadLocalChannel for our LoginEntry, which is directly managed
    //   by this class.
    Focus<SecurityFilter> secFilterFocus
      =parentFocus.<SecurityFilter>findFocus(SecurityFilter.FOCUS_URI);
    if (secFilterFocus!=null)
    { securityFilter=secFilterFocus.getSubject().get();
    }
    if (debug)
    {
      if (securityFilter==null)
      { log.fine("No security filter found");
      }
    }
    
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
  
  @Override
  protected Focus<?> bindExports()
    throws BindException
  {
    postSetters=bindAssignments(postAssignments);
    preSetters=bindAssignments(preAssignments);
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

