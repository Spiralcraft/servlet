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

import java.io.IOException;
import java.net.URI;
import spiralcraft.log.Level;

import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.lang.reflect.BeanReflector;

import spiralcraft.log.ClassLog;

import spiralcraft.net.smtp.SMTPConnector;
import spiralcraft.net.smtp.Envelope;

import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ServiceContext;

import spiralcraft.textgen.Generator;
import spiralcraft.util.ArrayUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

  
public class SMTP
  extends ControlGroup<Envelope>
{
  private static final ClassLog log
    =ClassLog.getInstance(SMTP.class);
  
  private Channel<SMTPConnector> smtpChannel;
  
  private Assignment<?>[] postAssignments;
  private Setter<?>[] postSetters;

  private Assignment<?>[] preAssignments;
  private Setter<?>[] preSetters;

  private Generator generator;
  private Resource templateResource;
  
  private Expression<String> senderX;
  private Expression<String> recipientX;
  
  /**
   * <p>Specify the URI of the textgen email template for the message body
   * </p>
   * @param templateURI
   */
  public void setTemplateURI(URI templateURI)
  { 
    try
    { 
      this.templateResource
        =Resolver.getInstance().resolve(templateURI);
    }
    catch (UnresolvableURIException x)
    { throw new IllegalArgumentException(x);
    }
    
  }
  
  public void setSenderX(Expression<String> senderX)
  { this.senderX=senderX;
  }
  
  public void setRecipientX(Expression<String> recipientX)
  { this.recipientX=recipientX;
  }

  /**
   * <p>Assignments which get executed prior to a login attempt (eg. to resolve
   *   credentials)
   * </p>
   * 
   * @param assignments
   */
  public void setPreAssignments(Assignment<Object>[] assignments)
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
  public void setAssignments(Assignment<Object>[] assignments)
  { this.postAssignments=assignments;
  }  
  
  protected void newEntry()
  { 
    getState().setValue(new Envelope());
  }  
  

  @Override
  protected Channel<?> bindTarget(Focus<?> parentFocus)
    throws BindException
  {
    Focus<SMTPConnector> smtpFocus
      =parentFocus.<SMTPConnector>
        findFocus(URI.create("class:/spiralcraft/net/smtp/SMTPConnector"));
    if (smtpFocus!=null)
    { smtpChannel=smtpFocus.getSubject();
    } 
    
    return new AbstractChannel<Envelope>
      (BeanReflector.<Envelope>getInstance(Envelope.class))
        {
          @Override
          protected Envelope retrieve()
          { return null;
          }

          @Override
          protected boolean store(Envelope val) throws AccessException
          { return false;
          }
        };
        

  }  
  
  public Command<Envelope,Void> sendCommand()
  {     
    return new CommandAdapter<Envelope,Void>()
    { 
      @Override
      public void run()
      { 
        try
        { send();
        }
        catch (IOException x)
        { setException(x);
        }
      }
    };
  }  

  public Command<Envelope,Void> sendCommand(final Command<?,?> successCommand)
  {     
    return new CommandAdapter<Envelope,Void>()
    { 
      @Override
      public void run()
      { 
        try
        { 
          send();
          if (getState().getErrors()==null)
          { successCommand.execute();
          } 
        }
        catch (IOException x)
        { setException(x);
        }
        
        
      }
    };
  }  
  
  public void send()
    throws IOException
  {
    Envelope envelope=getState().getValue();
    Setter.applyArray(preSetters);

    envelope.setEncodedMessage(generator.render());
    if (generator.getException()==null)
    {
      if (envelope.getSenderMailAddress()==null)
      { getState().addError("Missing sender address");
      }
      else if 
        (envelope.getRecipients()==null || envelope.getRecipients().isEmpty())
      { getState().addError("Missing recipients");
      }
      else
      {
        smtpChannel.get().send(envelope);
        Setter.applyArray(postSetters);
        if (debug)
        { 
          log.fine
            ("Sent to "+envelope.getRecipients()
            +"\r\n"+envelope.getEncodedMessage()
            );
        }
      
      }
    }
    else
    { getState().setException(generator.getException());
    }
    if (debug && getState().getErrors()!=null)
    { log.fine(ArrayUtil.format(getState().getErrors(),",",null));
    }
  }
  

  @SuppressWarnings("unchecked")
  protected void addPreAssignment(String targetX,Expression source)
  { 
    Assignment<?> assignment
      =new Assignment
        (Expression.create(targetX)
        ,source
        );
    
    preAssignments
      =preAssignments!=null
      ?ArrayUtil.append
        (preAssignments
        ,assignment
        )
       :new Assignment[] {assignment}
       ;
    
  }
      
  @Override
  protected Focus<?> bindExports()
    throws BindException
  {
    if (senderX!=null)
    { addPreAssignment("sender",senderX);
    }
    
    if (recipientX!=null)
    { addPreAssignment("recipient",recipientX);
    }

    postSetters=bindAssignments(postAssignments);
    preSetters=bindAssignments(preAssignments);
    generator
      =new Generator
        (templateResource
        ,getFocus()
        );      
    if (generator.getException()!=null)
    { 
      log.log
        (Level.SEVERE
        ,"Error compiling email template "+templateResource.getURI()
        ,generator.getException()
        );
    }
    return super.bindExports();
    
  }
  
  @Override
  protected void scatter(ServiceContext context)
  { 
    Envelope lastEntry=getState().getValue();
   
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
