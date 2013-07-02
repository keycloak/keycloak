package org.keycloak;

import java.security.KeyStore;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceMetadata
{
   protected String resourceName;
   protected String realm;
   protected KeyStore clientKeystore;
   protected String clientKeyPassword;
   protected KeyStore truststore;
   protected PublicKey realmKey;

   public String getResourceName()
   {
      return resourceName;
   }

   public String getRealm()
   {
      return realm;
   }

  /**
    * keystore that contains service's private key and certificate.
    * Used when making invocations on remote HTTPS endpoints that require client-cert authentication
    *
    * @return
    */
   public KeyStore getClientKeystore()
   {
      return clientKeystore;
   }

   public String getClientKeyPassword()
   {
      return clientKeyPassword;
   }

   public void setClientKeyPassword(String clientKeyPassword)
   {
      this.clientKeyPassword = clientKeyPassword;
   }

   /**
    * Truststore to use if this service makes client invocations on remote HTTPS endpoints.
    *
    * @return
    */
   public KeyStore getTruststore()
   {
      return truststore;
   }

   /**
    * Public key of the realm.  Used to verify access tokens
    *
    * @return
    */
   public PublicKey getRealmKey()
   {
      return realmKey;
   }

   public void setResourceName(String resourceName)
   {
      this.resourceName = resourceName;
   }

   public void setRealm(String realm)
   {
      this.realm = realm;
   }

   public void setClientKeystore(KeyStore clientKeystore)
   {
      this.clientKeystore = clientKeystore;
   }

   public void setTruststore(KeyStore truststore)
   {
      this.truststore = truststore;
   }

   public void setRealmKey(PublicKey realmKey)
   {
      this.realmKey = realmKey;
   }
}
