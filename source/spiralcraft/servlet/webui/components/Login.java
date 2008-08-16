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

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import spiralcraft.codec.CodecException;
import spiralcraft.codec.text.Base64Codec;
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
import spiralcraft.net.http.VariableMap;

import spiralcraft.security.auth.AuthSession;
import spiralcraft.security.auth.LoginEntry;

import spiralcraft.servlet.autofilter.SecurityFilter;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ControlGroupState;
import spiralcraft.servlet.webui.QueuedCommand;
import spiralcraft.servlet.webui.ServiceContext;


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
  
  private int minutesToPersist;
  
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
   * The amount of time a login should persist. Setting this enables
   *   persistent login cookies.
   */
  public void setMinutesToPersist(int minutesToPersist)
  { this.minutesToPersist=minutesToPersist;
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
    if (preSetters!=null)
    {  
      if (debug)
      { log.fine(toString()+": applying pre-assignments before login");
      }
      for (Setter<?> setter: preSetters)
      {  setter.set();
      }
    }
    if (!sessionChannel.get().authenticate())
    { 
      if (interactive)
      {
        state.setError
          (failureMessage);
      }
    }
    else
    { 
      if (securityFilter!=null)
      { 
        Cookie cookie=createLoginCookie(state.getValue());
        if (cookie!=null)
        { 
          if (debug)
          { log.fine("Setting login cookie "+cookie);
          }
          securityFilter.writeLoginCookie(cookie);
        }
      }
      
      state.setValue(null);
      newEntry();
      if (postSetters!=null)
      {
        if (debug)
        { log.fine(toString()+": applying post assignments on login");
        }
        for (Setter<?> setter: postSetters)
        { setter.set();
        }
      }            
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
    if (minutesToPersist > 0
        && securityFilter!=null
        && !session.isAuthenticated()
        )
    {
      // Only process a login cookie if we are persisting logins
      
      Cookie loginCookie=null;
      String cookieName=securityFilter.getCookieName();
      
      // Check for a cookie
      Cookie[] cookies=context.getRequest().getCookies();
      if (cookies!=null)
      {
        for (Cookie cookie:context.getRequest().getCookies())
        { 
          if (cookie.getName().equals(cookieName))
          { 
            loginCookie=cookie;
            break;
          }
        }
      }
      
      
      if (loginCookie!=null && readLoginCookie(loginCookie))
      { login(false);
      }
    }
    
    if (!state.isErrorState() && session.isAuthenticated())
    {  
      if (!inPlace)
      {
        try
        { context.redirect(URI.create(state.getReferer()));
        }
        catch (ServletException x)
        { state.setException(x);
        }
      }
    }
    
  }

  /**
   * <p>Read the login cookie and return whether login data was successfully
   *  read
   * </p>
   * 
   * @param cookie
   * @return Whether login data was successfully read
   */
  private boolean readLoginCookie(Cookie cookie)
  {
    VariableMap map=VariableMap.fromUrlEncodedString(cookie.getValue());
    LoginEntry entry=getState().getValue();
    String username=map.getOne("username");
    String ticketBase64=map.getOne("ticket");
    if (username!=null && ticketBase64!=null)
    {
      entry.setUsername(username);
      try
      { entry.setOpaqueDigest(Base64Codec.decodeBytes(ticketBase64));
      }
      catch (IOException x)
      { 
        x.printStackTrace();
        return false;
      }
      catch (CodecException x)
      {
        x.printStackTrace();
        return false;
      }
      return true;
    }
    else
    { return false;
    }
  }
  
  private Cookie createLoginCookie(LoginEntry entry)
  {
    if (minutesToPersist==0 || securityFilter==null)
    { return null;
    }
    else
    {
      
      VariableMap map=new VariableMap();
      String username=entry.getUsername();
      String password=entry.getPasswordCleartext();
      if (username!=null && password!=null)
      {
        byte[] ticket=sessionChannel.get().opaqueDigest(username+password);
        map.add("username", entry.getUsername());
        try
        { map.add("ticket", Base64Codec.encodeBytes(ticket));
        }
        catch (IOException x)
        {
          x.printStackTrace();
          return null;
        }
        catch (CodecException x)
        {
          x.printStackTrace();
          return null;
        }
        String data=map.generateEncodedForm();
        Cookie cookie=new Cookie(securityFilter.getCookieName(),data);
        cookie.setMaxAge(minutesToPersist*60); // Convert from seconds
        return cookie;
      }
      else
      { 
        if (debug)
        { 
          log.fine
            ("Some credentials were unspecified- no login cookie created");
        }
        return null;
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
   

  @SuppressWarnings("unchecked")
  @Override
  protected Channel<?> bindTarget(Focus<?> parentFocus)
    throws BindException
  {
    // Only used to provide a reflector to ControlGroup, so it can make
    //   a ThreadLocalChannel for our LoginEntry, which is directly managed
    //   by this class.
    Focus<SecurityFilter> secFilterFocus
      =(Focus<SecurityFilter>) parentFocus.findFocus(SecurityFilter.FOCUS_URI);
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

