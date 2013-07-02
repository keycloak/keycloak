package org.keycloak.services.model.data;

import org.bouncycastle.openssl.PEMWriter;
import org.jboss.resteasy.security.PemUtils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public class RealmModel implements Serializable
{
   private static final long serialVersionUID = 1L;

   protected String id;
   protected String name;
   protected long tokenLifespan = 3600 * 24; // one day
   protected long accessCodeLifespan = 300; // 5 minutes
   protected boolean enabled;
   protected boolean sslNotRequired;
   protected boolean cookieLoginAllowed;
   protected String publicKeyPem;
   protected String privateKeyPem;
   protected volatile transient PublicKey publicKey;
   protected volatile transient PrivateKey privateKey;

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

   public boolean isEnabled()
   {
      return enabled;
   }

   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }

   public boolean isSslNotRequired()
   {
      return sslNotRequired;
   }

   public void setSslNotRequired(boolean sslNotRequired)
   {
      this.sslNotRequired = sslNotRequired;
   }

   public boolean isCookieLoginAllowed()
   {
      return cookieLoginAllowed;
   }

   public void setCookieLoginAllowed(boolean cookieLoginAllowed)
   {
      this.cookieLoginAllowed = cookieLoginAllowed;
   }

   public long getTokenLifespan()
   {
      return tokenLifespan;
   }

   public void setTokenLifespan(long tokenLifespan)
   {
      this.tokenLifespan = tokenLifespan;
   }

   public long getAccessCodeLifespan()
   {
      return accessCodeLifespan;
   }

   public void setAccessCodeLifespan(long accessCodeLifespan)
   {
      this.accessCodeLifespan = accessCodeLifespan;
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

   public String getPrivateKeyPem()
   {
      return privateKeyPem;
   }

   public void setPrivateKeyPem(String privateKeyPem)
   {
      this.privateKeyPem = privateKeyPem;
      this.privateKey = null;
   }

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

   public PrivateKey getPrivateKey()
   {
      if (privateKey != null) return privateKey;
      if (privateKeyPem != null)
      {
         try
         {
            privateKey = PemUtils.decodePrivateKey(privateKeyPem);
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }
      return privateKey;
   }

   public void setPrivateKey(PrivateKey privateKey)
   {
      this.privateKey = privateKey;
      StringWriter writer = new StringWriter();
      PEMWriter pemWriter = new PEMWriter(writer);
      try
      {
         pemWriter.writeObject(privateKey);
         pemWriter.flush();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      String s = writer.toString();
      this.privateKeyPem = PemUtils.removeBeginEnd(s);
   }
}
