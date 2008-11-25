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
package spiralcraft.servlet.autofilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import spiralcraft.data.DataComposite;
import spiralcraft.data.Type;
import spiralcraft.data.session.DataSession;
import spiralcraft.data.session.DataSessionFocus;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;

import spiralcraft.log.ClassLog;


/**
 * Create a session scoped DataSession and state managed to allow data
 *   bindings. 
 *   
 *   
 * 
 * @author mike
 *
 */
public class DataSessionFilter
  extends FocusFilter<DataSession>
{
  
  private static final ClassLog log
    =ClassLog.getInstance(DataSessionFilter.class);
  
  private ThreadLocalChannel<DataSession> dataSessionChannel;
  private String attributeName;
  private Type<DataComposite> dataType;
  
  
  public void setDataType(Type<DataComposite> dataType)
  { this.dataType=dataType;
  }
  
  /**
   * Called -once- to create the Focus
   */
  @Override
  protected Focus<DataSession> createFocus
    (Focus<?> parentFocus)
    throws BindException
  { 
    
    this.attributeName=this.getPath().format("/")+"!"+
      (dataType!=null?dataType.getURI():"");
    
    // XXX Replace with XML binding
    dataSessionChannel
      =new ThreadLocalChannel<DataSession>
        (BeanReflector.<DataSession>getInstance(DataSession.class));
    
    DataSessionFocus dataSessionFocus
      =new DataSessionFocus(parentFocus,dataSessionChannel,dataType);
    return dataSessionFocus;
  }
  
  @Override
  protected void popSubject(HttpServletRequest request)
  {
    dataSessionChannel.pop();
    
  }
  
  

  @Override
  protected void pushSubject
    (HttpServletRequest request,HttpServletResponse response) 
    throws BindException
  {
      
    HttpSession session=request.getSession();
    DataSession dataSession
      =(DataSession) session.getAttribute(attributeName);
      
    boolean newDataSession=false;
    if (dataSession==null)
    { 
      // Avoid race condition
      synchronized (session)
      {
        dataSession
          =(DataSession) session.getAttribute(attributeName);
        if (dataSession==null)
        {
          newDataSession=true;
          dataSession=((DataSessionFocus) getFocus()).newDataSession();
          session.setAttribute(attributeName,dataSession);
          if (debug)
          { 
            log.fine
              ("New dataSession created: "
              +dataSession.getType().getURI()
              +" in http session "+session.getId()
              );
          }
          
        }
        else
        {
          if (debug)
          { 
            log.fine
              ("Race condition averted for dataSession "
              +dataSession.getType().getURI()
              +" in http session "+session.getId()
              );
          }
        }
      }  
    }
    dataSessionChannel.push(dataSession);  
    if (newDataSession)
    { ((DataSessionFocus) getFocus()).initializeDataSession();
    }
  }

}
