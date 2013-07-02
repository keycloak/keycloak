package org.keycloak.representations.idm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RequiredCredentialRepresentation
{
   public static final String PASSWORD = "Password";
   public static final String TOTP = "TOTP";
   public static final String CLIENT_CERT = "CLIENT_CERT";
   public static final String CALLER_PRINCIPAL = "CALLER_PRINCIPAL";
   protected String type;
   protected boolean input;
   protected boolean secret;

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
