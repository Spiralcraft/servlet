//
//Copyright (c) 2010 Michael Toth
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.Level;
import spiralcraft.servlet.autofilter.spi.RequestFocusFilter;

import spiralcraft.profiler.ProfilerAgent;

/**
 * Puts the result of an expression into the Focus chain
 * 
 * @author mike
 *
 */
public class ProfilerFilter
  extends RequestFocusFilter<ProfilerAgent>
{
  
  private Binding<Boolean> profileEnableX;
  private Channel<HttpServletRequest> requestChannel;
  
  /**
   * Specify the Expression to bind
   * 
   * @param x
   */
  public void setProfileEnableX(Binding<Boolean> profileEnableX)
  { this.profileEnableX=profileEnableX;
  }
  
  @Override
  protected ProfilerAgent createValue(
    HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException
  { 
   
    if (Boolean.TRUE==profileEnableX.get())
    { 
      ProfilerAgent agent=new ProfilerAgent();
      agent.start();
   
      return agent;
    }
    else
    { return null;
    }
    
  }

  @Override
  protected Reflector<ProfilerAgent> resolveReflector(
    Focus<?> parentFocus)
    throws BindException
  { 
    profileEnableX.bind(parentFocus);
    requestChannel
      =LangUtil.<HttpServletRequest>findChannel(HttpServletRequest.class,parentFocus);
    return BeanReflector.<ProfilerAgent>getInstance(ProfilerAgent.class);
  }
  
  

  protected void releaseValue()
  { 
    HttpServletRequest request=requestChannel.get();
    ProfilerAgent agent=channel.get();
    if (agent!=null)
    {
      agent.stop();
      StringBuilder report=new StringBuilder();
      report.append("PROFILED ");
      report.append(request.getRequestURL().toString());
      report.append("\r\n");
      try
      { agent.generateReport(report,null);
      }
      catch (IOException x)
      {
      }
      log.log(Level.TRACE,report.toString(),null);
    }
  }
}
