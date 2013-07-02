package org.keycloak.services.model.data;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public class RequiredCredentialModel implements Serializable
{
   private static final long serialVersionUID = 1L;

   protected String id;
   protected String type;
   protected boolean input;
   protected boolean secret;

   public String getId()
   {
      return id;
   }

   public void setId(String id)
   {
      this.id = id;
   }

   public String getType()
   {
      return type;
   }

   public void setType(String type)
   {
      this.type = type;
   }

   public boolean isInput()
   {
      return input;
   }

   public void setInput(boolean input)
   {
      this.input = input;
   }

   public boolean isSecret()
   {
      return secret;
   }

   public void setSecret(boolean secret)
   {
      this.secret = secret;
   }
}
