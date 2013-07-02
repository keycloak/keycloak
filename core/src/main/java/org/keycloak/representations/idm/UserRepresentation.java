package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class UserRepresentation
{
   public static class Credential
   {
      protected String type;
      protected String value;
      protected boolean hashed;

      public String getType()
      {
         return type;
      }

      public void setType(String type)
      {
         this.type = type;
      }

      public String getValue()
      {
         return value;
      }

      public void setValue(String value)
      {
         this.value = value;
      }

      public boolean isHashed()
      {
         return hashed;
      }

      public void setHashed(boolean hashed)
      {
         this.hashed = hashed;
      }
   }

   protected String self; // link
   protected String username;
   protected boolean enabled;
   protected Map<String, String> attributes;
   protected List<Credential> credentials;

   public String getSelf()
   {
      return self;
   }

   public void setSelf(String self)
   {
      this.self = self;
   }

   public String getUsername()
   {
      return username;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }

   public Map<String, String> getAttributes()
   {
      return attributes;
   }

  public void setAttributes(Map<String, String> attributes)
   {
      this.attributes = attributes;
   }

   public List<Credential> getCredentials()
   {
      return credentials;
   }

   public void setCredentials(List<Credential> credentials)
   {
      this.credentials = credentials;
   }

   public UserRepresentation attribute(String name, String value)
   {
      if (this.attributes == null) attributes = new HashMap<String, String>();
      attributes.put(name, value);
      return this;
   }

   public UserRepresentation credential(String type, String value, boolean hashed)
   {
      if (this.credentials == null) credentials = new ArrayList<Credential>();
      Credential cred = new Credential();
      cred.setType(type);
      cred.setValue(value);
      cred.setHashed(hashed);
      credentials.add( cred);
      return this;
   }

   public boolean isEnabled()
   {
      return enabled;
   }

   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }
}
