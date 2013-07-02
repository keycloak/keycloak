package org.keycloak.services.model.data;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public class ResourceModel implements Serializable
{
   private static final long serialVersionUID = 1L;
   protected String id;
   protected String name;
   protected boolean surrogateAuthRequired;

   public String getId()
   {
      return id;
   }

   public void setId(String id)
   {
      this.id = id;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public boolean isSurrogateAuthRequired()
   {
      return surrogateAuthRequired;
   }

   public void setSurrogateAuthRequired(boolean surrogateAuthRequired)
   {
      this.surrogateAuthRequired = surrogateAuthRequired;
   }
}
