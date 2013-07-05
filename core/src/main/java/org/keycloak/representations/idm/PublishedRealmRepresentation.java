package org.keycloak.representations.idm;

import org.bouncycastle.openssl.PEMWriter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.keycloak.PemUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PublishedRealmRepresentation
{
   protected String realm;
   protected String self;

   @JsonProperty("public_key")
   protected String publicKeyPem;

   @JsonProperty("authorization")
   protected String authorizationUrl;

   @JsonProperty("codes")
   protected String codeUrl;

   @JsonProperty("grants")
   protected String grantUrl;

   @JsonProperty("identity-grants")
   protected String identityGrantUrl;

   @JsonIgnore
   protected volatile transient PublicKey publicKey;


   public String getRealm()
   {
      return realm;
   }

   public void setRealm(String realm)
   {
      this.realm = realm;
   }

   public String getSelf()
   {
      return self;
   }

   public void setSelf(String self)
   {
      this.self = self;
   }

   public String getPublicKeyPem()
   {
      return publicKeyPem;
   }

   public void setPublicKeyPem(String publicKeyPem)
   {
      this.publicKeyPem = publicKeyPem;
      this.publicKey = null;
   }


   @JsonIgnore
   public PublicKey getPublicKey()
   {
      if (publicKey != null) return publicKey;
      if (publicKeyPem != null)
      {
         try
         {
            publicKey = PemUtils.decodePublicKey(publicKeyPem);
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }
      return publicKey;
   }

   @JsonIgnore
   public void setPublicKey(PublicKey publicKey)
   {
      this.publicKey = publicKey;
      StringWriter writer = new StringWriter();
      PEMWriter pemWriter = new PEMWriter(writer);
      try
      {
         pemWriter.writeObject(publicKey);
         pemWriter.flush();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      String s = writer.toString();
      this.publicKeyPem = PemUtils.removeBeginEnd(s);
   }


   public String getAuthorizationUrl()
   {
      return authorizationUrl;
   }

   public void setAuthorizationUrl(String authorizationUrl)
   {
      this.authorizationUrl = authorizationUrl;
   }

   public String getCodeUrl()
   {
      return codeUrl;
   }

   public void setCodeUrl(String codeUrl)
   {
      this.codeUrl = codeUrl;
   }

   public String getGrantUrl()
   {
      return grantUrl;
   }

   public void setGrantUrl(String grantUrl)
   {
      this.grantUrl = grantUrl;
   }

   public String getIdentityGrantUrl()
   {
      return identityGrantUrl;
   }

   public void setIdentityGrantUrl(String identityGrantUrl)
   {
      this.identityGrantUrl = identityGrantUrl;
   }
}
