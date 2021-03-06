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



import spiralcraft.app.Dispatcher;
import spiralcraft.command.Command;
import spiralcraft.command.CommandAdapter;
import spiralcraft.common.ContextualException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Binding;
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
import spiralcraft.servlet.webui.ServiceRootComponent;


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
  private ServiceRootComponent root;
  
  private int pageSize=10;
  private Binding<Integer> pageControlX;
  private Binding<Integer> pageSizeX;
  
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
  
  /**
   * An external source for the page number
   * 
   * @param pageControlX
   */
  public void setPageControlX(Binding<Integer> pageControlX)
  { 
    this.removeParentContextual(this.pageControlX);
    this.pageControlX=pageControlX;
    this.addParentContextual(this.pageControlX);
  }
  
  /**
   * An external source for the page size
   * 
   * @param pageControlX
   */
  public void setPageSizeX(Binding<Integer> pageSizeX)
  {
    this.removeParentContextual(this.pageSizeX);
    this.pageSizeX=pageSizeX;
    this.addParentContextual(this.pageSizeX);
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
  protected Action createResetAction(Dispatcher context)
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
            +getTargetPath().format(".")
            );
        }
        getState(context).setCurrentPage(0);
        root.dirty();
      }
    };
  }  
    
  @SuppressWarnings("unchecked")
  @Override
  protected void scatter(ServiceContext context)
  {
    super.scatter(context);
    if (context.getInitial() && !context.isSameReferer())
    {
      PageState<Ttarget,Titem> state=getState(context);
      state.setCurrentPage(0);
    }
    if (pageControlX!=null)
    {
      Integer pageNum=pageControlX.get();
      if (pageNum!=null)
      { 
        PageState<Ttarget,Titem> state=getState(context);
        state.setCurrentPage(pageNum.intValue());
      }
    }
    resetPageState((PageState<Ttarget,Titem>) context.getState());
  }
  
  
  @SuppressWarnings("unchecked") // PageItem cast
  protected void resetPageState(PageState<Ttarget,Titem> state)
  {
    state.setPageSize(getResolvedPageSize());  
    int start=state.getCurrentPage()*state.getPageSize();

    int count=0;
    
    ArrayList<Titem> list=new ArrayList<Titem>(state.getPageSize());

    
    if (listDecorator!=null)
    {
      Ttarget targetList=target.get();
      count=targetList==null?0:listDecorator.size(targetList);
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
    throws ContextualException
  {
    root=this.findComponent(ServiceRootComponent.class);
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

  /**
   * The number of pages
   * 
   * @return
   */
  public int getPageCount()
  { return getState().getPageCount();
  }
  
  /**
   * An array of page numbers
   * 
   * @return
   */
  public Integer[] getPageList()
  { return getState().getPageList();
  }

  /**
   * The number of items across all pages
   * 
   * @return
   */
  public int getItemCount()
  { return getState().getItemCount();
  }

  /**
   * The current page number
   * 
   * @return
   */
  public int getCurrentPage()
  { return getState().getCurrentPage();
  }

  /**
   * The data on the current page
   * 
   * @return
   */
  public Titem[] getPageData()
  { return getState().getPageData();
  }

  @Override
  public PageState<Ttarget,Titem> createState()
  { return new PageState<Ttarget,Titem>(this);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected PageState<Ttarget,Titem> getState(Dispatcher context)
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
  
  public int getResolvedPageSize()
  { 
    Integer pageSize=pageSizeX!=null?pageSizeX.get():null;
    return pageSize!=null?pageSize:getPageSize();
  }
}

