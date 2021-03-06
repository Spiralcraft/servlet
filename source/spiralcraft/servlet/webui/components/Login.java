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
package spiralcraft.servlet.webui.components;

import java.net.URI;
import spiralcraft.log.Level;

import javax.servlet.ServletException;

import spiralcraft.app.Dispatcher;
import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.common.ContextualException;


import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.spi.NullChannel;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.log.ClassLog;

import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.LoginEntry;
import spiralcraft.security.auth.Permission;

import spiralcraft.servlet.autofilter.SecurityFilter;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.RandomUtil;


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

  private static final Permission LOGIN_PERMISSION;
  static
  {
    try
    { 
      LOGIN_PERMISSION=LangUtil.eval
        ("[@:class:/spiralcraft/security/auth/LoginPermission].()");
    }
    catch (BindException x)
    { throw new RuntimeException("Error resolving LoginPermission",x);
    } 
  }
      
  private static final ClassLog log
    =ClassLog.getInstance(Login.class);

  private Channel<AuthSession> sessionChannel;
  private URI defaultURI;

  private Assignment<?>[] postAssignments;
  private Setter<?>[] postSetters;

  private Assignment<?>[] preAssignments;
  private Setter<?>[] preSetters;

  private Assignment<?>[] defaultAssignments;
  private Setter<?>[] defaultSetters;

  private boolean inPlace;
  private String failureMessage
    ="Login failed, username/password combination not recognized";
  
  
  private SecurityFilter securityFilter;
  private Binding<?> onLoginX;
  
  private boolean requireLoginPermission;
  private boolean useChallenge;
  
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
   * <p>Indicate that the LoginPermission must be assigned to the user in
   *   order for the login to succeed. If no LoginPermission is assigned,
   *   a permission error will be indicated and the user will not be allowed
   *   to login. 
   * </p>
   * 
   * <p>Defaults to false, indicating that successful authentication is 
   *   sufficient to allow login.
   * </p>
   * 
   * @param requireLoginPermission
   */
  public void setRequireLoginPermission(boolean requireLoginPermission)
  { this.requireLoginPermission=requireLoginPermission;
  }
  
  
  /**
   * <p>Specify that the login form is an in-place login that may be present
   *   inside the secured page, and thus no automatic redirect to a 
   *   referring page should occur.
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

  public Command<LoginEntry,Void,Void> loginCommand()
  {     
    return new QueuedCommand<LoginEntry,Void,Void>
      (getState()
      ,new CommandAdapter<LoginEntry,Void,Void>()
        { 
          @Override
          public void run()
          { login(true);
          }
        }
      );
  }
  
  public Command<LoginEntry,Void,Void> loginCommand(final Command<?,?,?> onSuccess)
  {     
    return new QueuedCommand<LoginEntry,Void,Void>
      (getState()
      ,new CommandAdapter<LoginEntry,Void,Void>()
        { 
          @Override
          public void run()
          { 
            if (login(true))
            { 
              onSuccess.execute();
              if (onSuccess.getException()!=null)
              { setException(onSuccess.getException());
              }
            }
          }
        }
      );
  }

  public void login()
  { 
    getState().queueCommand
      (new CommandAdapter<LoginEntry,Void,Void>()
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
  
  @SuppressWarnings("unchecked")
  @Override
  protected LoginState getState(Dispatcher context)
  { return (LoginState) context.getState();
  }

  private boolean login(boolean interactive)
  {
    LoginState state=(LoginState) getState();
    state.setActioned(true);
    if (interactive)
    { 
      // Make sure we take into account anything that's been updated
      //   since we gathered.
      state.getValue().update();
    }
    
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
    String username=null;
    
    synchronized (session)
    {
      
      authenticated=session.authenticate();
     
      if (authenticated)
      { 
        username=session.getPrincipal().getName(); 
        if (requireLoginPermission)
        { 
          authenticated=session.hasPermission(LOGIN_PERMISSION);
          if (authenticated)
          { 
            if (debug)
            { log.fine(getLogPrefix()+username+" has LoginPermission");
            }
          }
          else
          { 
            session.logout();
            if (debug)
            { log.fine(getLogPrefix()+username+" does NOT have LoginPermission");
            }
            
          }
        }

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
      return false;
    }
    else
    { 
      if (debug)
      { 
        log.fine("Successful login for "
          +username
          );
      }

      if (securityFilter!=null 
          && (securityFilter.getRequireValidCookie()
              || state.getValue().isPersistent() 
             )
         )
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
      
      if (onLoginX!=null)
      {
        try
        {
          Object val=onLoginX.get();
          if (debug)
          { log.fine(toString()+": onLogin returned "+val);
          }
        }
        catch (RuntimeException x)
        { 
          state.setException(x);
          log.log(Level.WARNING,"Login.onLogin threw exception",x);
          throw x;
        }
      }
      
      state.setValue(null);
      newEntry();
      return true;
    }
      
  }
 
  
  private void computeReferer(ServiceContext context,LoginState state)
  {
    if ( (state.getValue()==null 
          && state.getReferer()==null
          )
       || 
          (context.getInitial()
          && !context.isSameReferer()
          )
       )
    { 
      
      String refererParam
        =context.getQuery()!=null?context.getQuery().getFirst("referer"):null;
      
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
    
  }
  
  
  @Override
  protected void handlePrepare(ServiceContext context)
  { 
    
    LoginState state=getState(context);
    computeReferer(context,state);
    super.handlePrepare(context);
    
    AuthSession session=sessionChannel.get();
    
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
        if (state.getActioned())
        {
          if (debug)
          { log.fine("Redirecting to "+state.getReferer());
          }          
          // Only the form that got the credentials will have a
          //   username and trigger the redirect
          try
          { context.redirect(URI.create(state.getReferer()));
          }
          catch (ServletException x)
          { handleException(context,x);
          }
        }
        else
        {
          if (debug)
          { 
            log.fine("Inactive Login component- NOT redirecting to "
              +state.getReferer()
              );
          }
        }
      }
    }
    else
    {
      if (debug)
      { log.fine("Session is not authenticated "+session);
      }
    }
    state.setActioned(false);
    
  }


  

  
  /**
   * <p>Assignments which get executed when a new LoginEntry is created
   * </p>
   * 
   * @param assignments
   */
  public void setDefaultAssignments(Assignment<?>[] assignments)
  { this.defaultAssignments=assignments;
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
   * 
   * @param assignments
   * @Deprecated: Use setPostAssignments()
   */
  @Deprecated
  public void setAssignments(Assignment<?>[] assignments)
  { 
    log.fine("Login.assignments is deprecated. Use Login.postAssignments");
    this.postAssignments=assignments;
  }  
  
  /**
   * An expression to be evaluated after a successful login
   * 
   * @param x
   */
  public void setOnLogin(Binding<?> onLoginX)
  { this.onLoginX=onLoginX;
  }
  
  /**
   * <p>Assignments which get executed immediately after a successful login
   * </p>
   * 
   * @param assignments
   */
  public void setPostAssignments(Assignment<?>[] assignments)
  { this.postAssignments=assignments;
  }
  
  /**
   * Whether a challenge will be generated to ensure additional transport
   *   security of password digest credential.
   * 
   * @param useChallenge
   */
  public void setUseChallenge(boolean useChallenge)
  { this.useChallenge=useChallenge;
  }
  
  protected void newEntry()
  { 
    LoginEntry loginEntry=new LoginEntry(sessionChannel);
    if (useChallenge)
    { loginEntry.setChallenge(RandomUtil.generateString(32));
    }
    getState().setValue(loginEntry);
    if (defaultSetters!=null)
    { Setter.applyArrayIfNull(defaultSetters);
    }
    
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
    return new NullChannel<LoginEntry>
      (BeanReflector.<LoginEntry>getInstance(LoginEntry.class));     

  }
  
  @Override
  protected Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  {
    postSetters=bindAssignments(focus,postAssignments);
    preSetters=bindAssignments(focus,preAssignments);
    defaultSetters=bindAssignments(focus,defaultAssignments);
    if (onLoginX!=null)
    { onLoginX.bind(focus);
    }
    return super.bindExports(focus);
    
  }
  
//  @Override
//  protected void preGather(ServiceContext context)
//  { 
//    super.preGather(context);
//    if (debug)
//    { log.fine("Resetting LoginEntry");
//    }
//    LoginState state=getState(context);
//    if (state.getValue()!=null)
//    { state.getValue().reset();
//    }
//    
//  }
  
  @Override
  protected void gather(ServiceContext context)
  {
    super.gather(context);
    LoginState state=getState(context);
    if (state.getValue()!=null)
    { 
      if (debug)
      { log.fine("Updating LoginEntry");
      }
      state.getValue().update();
    }
    
  }
  
  @Override
  protected void scatter(ServiceContext context)
  { 
    LoginState state=getState(context);
    LoginEntry lastEntry=state.getValue();
   
    super.scatter(context);
    computeReferer(context,state);
    if (state.getValue()==null)
    { 
      if (lastEntry==null)
      { newEntry();
      }
      else
      { state.setValue(lastEntry);
      }
    }
    
    LoginEntry currentEntry=state.getValue();
    String username=currentEntry.getUsername();
    
    // Figure out whether the user has a login ticket
    AuthSession session=sessionChannel.get();    
    if (securityFilter!=null
        && !session.isAuthenticated()
        && !state.isErrorState()
        )
    { 
      if (debug)
      { log.fine("Not authenticated- checking cookie");
      }
      
      if (securityFilter.readLoginCookie(currentEntry))
      {
        // SecurityFilter only processes a login cookie if we are persisting
        //   logins
        
        if (debug)
        { log.fine("Logging in from cookie");
        }

        if (!login(false))
        { 
          if (debug)
          { log.fine("Resetting LoginEntry for "+username);
          }
          // Don't keep around failed credentials from ticket, but preserve
          //   interactive username
          currentEntry.reset();
          currentEntry.setUsername(username);
        }
      }      
    }
    
    
  } 
  
}



