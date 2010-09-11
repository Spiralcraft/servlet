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

import java.lang.reflect.Array;
import java.util.ArrayList;



import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.Focus;
import spiralcraft.lang.IterationCursor;
import spiralcraft.lang.IterationDecorator;
import spiralcraft.lang.ListDecorator;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.lang.reflect.ArrayReflector;

import spiralcraft.log.ClassLog;


import spiralcraft.servlet.webui.Action;
import spiralcraft.servlet.webui.ControlGroup;
import spiralcraft.servlet.webui.ServiceContext;
import spiralcraft.textgen.EventContext;
import spiralcraft.util.ArrayUtil;


/**
 * Paginates data from a source
 * 
 * @author mike
 *
 */
public class Paginate<Ttarget,Titem>
  extends ControlGroup<Ttarget>
{
  private static final ClassLog log
    =ClassLog.getInstance(Paginate.class);

  private IterationDecorator<Ttarget,Titem> iterationDecorator;
  private ListDecorator<Ttarget,Titem> listDecorator;
  private Reflector<Titem> componentReflector;
  private String resetActionName;
  
  private int pageSize=10;
  
  @Override
  @SuppressWarnings("unchecked")
  public PageState<Ttarget,Titem> getState()
  { return (PageState<Ttarget,Titem>) super.getState();
  }
  
  public void setPageSize(int pageSize)
  { this.pageSize=pageSize;
  }
  
  public int getPageSize()
  { return pageSize;
  }
  
  public void setResetActionName(String resetActionName)
  { this.resetActionName=resetActionName;
  }
  
  @Override
  protected void handleInitialize(ServiceContext context)
  {
    super.handleInitialize(context);
    context.registerAction(createResetAction(context));    
  }
  
  /**
   * <p>Create a new Action to reset the paginator
   * </p>
   * 
   * @param context
   * @return A new Action
   */
  protected Action createResetAction(EventContext context)
  {
    String actionName
      =resetActionName==null
      ?getClass().getName()+(getId()!=null?"."+getId():"")+".reset"
      :resetActionName
      ;
      
    if (debug)
    { log.fine("Creating action "+actionName);
    }
    return new Action(actionName,context.getState().getPath())
    {

      { responsive=false;
      }
      
      @Override
      public void invoke(ServiceContext context)
      { 
        if (debug)
        {
          log.fine
            ("Paginate: Action invoked: "+getName()+"@"
            +ArrayUtil.format(getTargetPath(),".",null)
            );
        }
        getState(context).setCurrentPage(0);
        
      }
    };
  }  
    
  
  @Override
  protected void handlePrepare(ServiceContext context)
  { 
    
    
    super.handlePrepare(context);
    PageState<Ttarget,Titem> state=getState(context);
    resetPageState(state);
  }
  
  @SuppressWarnings("unchecked") // PageItem cast
  protected void resetPageState(PageState<Ttarget,Titem> state)
  {
   
      
    int start=state.getCurrentPage()*state.getPageSize();

    int count=0;
    
    ArrayList<Titem> list=new ArrayList<Titem>(state.getPageSize());

    
    if (listDecorator!=null)
    {
      Ttarget targetList=target.get();
      count=listDecorator.size(targetList);
      int max=Math.min(start+state.getPageSize(),count);
      for (int i=start;i<max;i++)
      { list.add(listDecorator.get(targetList,i));
      }
    }
    else
    {
      IterationCursor<Titem> cursor=iterationDecorator.iterator();
      while (cursor.hasNext())
      {
        Titem item=cursor.next();
        if (count>=start && count<start+state.getPageSize())
        { list.add(item);
        }
        count++;
      }
    }
    state.setItemCount(count);
    Titem[] pageData
      =list.toArray
        ((Titem[]) Array.newInstance
           (componentReflector.getContentType()
            ,list.size()
           )
        );
    
    state.setPageData(pageData);
    
    if (debug)
    { 
      log.fine
        ("Page="+state.getCurrentPage()+" pitems="
        +pageData.length+" items="+count
        );
    }
  }  
   

  @Override
  protected Channel<?> bindTarget(Focus<?> parentFocus)
    throws BindException
  {
    // We will receive something that can be Iterated over
    
    // We want to expose an Array of "elements" on the selected page
    
    // Optionally, we need a size() method.
    return super.bindTarget(parentFocus);
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" }) // Issues with genericizing decorate method
  protected Focus<?> bindExports(Focus<?> focus)
    throws BindException
  {
    Channel<Ttarget> targetChannel=(Channel<Ttarget>) focus.getSubject();

    
    listDecorator
      =targetChannel.<ListDecorator>decorate(ListDecorator.class);
    
    if (listDecorator!=null)
    {
      componentReflector
        =listDecorator.getComponentReflector();

    }
    else
    {
    
      iterationDecorator
        =targetChannel.<IterationDecorator>decorate(IterationDecorator.class);
    
      if (iterationDecorator!=null)
      {
        componentReflector
          =iterationDecorator.getComponentReflector();

      }
    }
    
    if (iterationDecorator==null && listDecorator==null)
    { 
      throw new BindException
        ("Paginate target does not provide a ListDecorator or "
        +" an IterationDecorator" +
        "- no means for traversing collection"
        );
    }
    if (debug)
    { log.fine
        ("decorator="+(listDecorator!=null?listDecorator:iterationDecorator));
    }
    
    Channel out
      =new AbstractChannel<Titem[]>
      (ArrayReflector.<Titem>getInstance(componentReflector))
        {
          @Override
          protected Titem[] retrieve()
          { return getState().getPageData();
          }

          @Override
          protected boolean store(Titem[] val) 
          { return false;
          }
        };
    
    SimpleFocus compoundFocus
      =new SimpleFocus(focus,out);
    
    compoundFocus.addFacet
      (this.getAssembly().getFocus());
    
    return compoundFocus;
  }
  
  

  @Override
  public PageState<Ttarget,Titem> createState()
  { return new PageState<Ttarget,Titem>(this);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected PageState<Ttarget,Titem> getState(EventContext context)
  { return (PageState<Ttarget,Titem>) context.getState();
  }

  public Command<?,?,?> currentPageCommand(final int num)
  { 
    return new CommandAdapter<PageState<Ttarget,Titem>,Object,Void>()
    {
      { setTarget(getState());
      }
      
      @Override
      public void run()
      { this.getTarget().setCurrentPage(num);
      }
    };
  }
  
  public Command<?,?,?> resetCommand()
  {
    return new CommandAdapter<PageState<Ttarget,Titem>,Object,Void>()
    {
      { setTarget(getState());
      }
      
      @Override
      public void run()
      { 
        this.getTarget().setCurrentPage(0);
      }
    };
  }
  
}

