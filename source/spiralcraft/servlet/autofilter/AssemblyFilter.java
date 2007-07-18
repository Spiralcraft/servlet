package spiralcraft.servlet.autofilter;

import java.net.URI;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Focus;
import spiralcraft.lang.FocusProvider;

import spiralcraft.data.persist.PersistenceException;
import spiralcraft.data.persist.XmlAssembly;

import spiralcraft.builder.LifecycleException;


/**
 * Creates a Focus from an Assembly via an spiralcraft.data.persist.XmlAssembly
 */
public abstract class AssemblyFilter<T>
    extends FocusFilter<T>
{

  private URI typeURI;
  private URI instanceURI;
  
  public void setTypeURI(URI typeURI)
  { this.typeURI=typeURI;
  }
  
  public void setInstanceURI(URI instanceURI)
  { this.instanceURI=instanceURI;
  }

  protected class FocusHolder
  {
    // XXX Register as session event listener
    
    private Focus<T> focus;
    private XmlAssembly<?> assembly;
    
    @SuppressWarnings("unchecked") // Runtime class check for Focus provider
    public FocusHolder()
      throws BindException
    { 
      try
      { this.assembly=new XmlAssembly(typeURI,instanceURI);
      }
      catch (PersistenceException x)
      { throw new BindException("Error creating XmlAssembly: "+x,x);
      }
      
      try
      { assembly.start();
      }
      catch (LifecycleException x)
      { throw new BindException("Error creating XmlAssembly: "+x,x);
      }
      
      Channel subject=assembly.getAssembly().getSubject();
      
      if (FocusProvider.class.isAssignableFrom(subject.getContentType()))
      {
        // Delegate so component can provide its own focus, which simply
        //   serves as a reference to the subject/context values that will
        //   be managed according to the specifics of the AssemblyFocusFilter
        //   subclass.
        
        focus=((FocusProvider<T>) subject.get())
          .createFocus(null,null,null);
      }
      else
      { focus= (Focus<T>) assembly.getAssembly();
      }
      
      
    }
    
    
    public Focus<T> getFocus()
    { return focus;
    }
    
    public void destroy()
      throws LifecycleException
    { assembly.stop();
    }
  }


}

