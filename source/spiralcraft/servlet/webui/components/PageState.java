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

import spiralcraft.servlet.webui.ControlGroupState;

public class PageState<Ttarget,Titem>
  extends ControlGroupState<Ttarget>
{
  
  private int pageSize;
  private int currentPage=0;
  private int itemCount=0;
  private Titem[] pageData;
  
  public PageState(Paginate<Ttarget,Titem> control)
  { 
    super(control);
    pageSize=control.getPageSize();
  }
  
  public int getPageSize()
  { return pageSize;
  }
  
  public void setPageSize(int pageSize)
  { this.pageSize=pageSize;
  }
  
  public int getCurrentPage()
  { return currentPage;
  }
  
  public void setCurrentPage(int page)
  { currentPage=page;
  }
  
  public int getPageCount()
  { return (int) Math.ceil(itemCount/pageSize);
  }
  
  public int getItemCount()
  { return itemCount;
  }
  
  public void setItemCount(int itemCount)
  { this.itemCount=itemCount;
  }
  
  public Titem[] getPageData()
  { return pageData;
  }
  
  public Integer[] getPageList()
  { 
    int cnt=getPageCount();
    Integer[] ret=new Integer[cnt];
    for (int i=0;i<cnt;i++)
    { ret[i]=i;
    }
    return ret;
  }
  
  public void setPageData(Titem[] pageData)
  { this.pageData=pageData;
  }
  
  
  
}

