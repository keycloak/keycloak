package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.jboss.resteasy.jwt.JsonWebToken;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyToken extends JsonWebToken
{
   public static class Access
   {
      @JsonProperty("roles")
      protected Set<String> roles;
      @JsonProperty("verify_caller")
      protected Boolean verifyCaller;

      public Set<String> getRoles()
      {
         return roles;
      }

      public Access roles(Set<String> roles)
      {
         this.roles = roles;
         return this;
      }

      @JsonIgnore
      public boolean isUserInRole(String role)
      {
         if (roles == null) return false;
         return roles.contains(role);
      }

      public Access addRole(String role)
      {
         if (roles == null) roles = new HashSet<String>();
         roles.add(role);
         return this;
      }

      public Boolean getVerifyCaller()
      {
         return verifyCaller;
      }

      public Access verifyCaller(Boolean required)
      {
         this.verifyCaller = required;
         return this;
      }
   }

   @JsonProperty("issuedFor")
   public String issuedFor;

   @JsonProperty("trusted-certs")
   protected Set<String> trustedCertificates;


   @JsonProperty("realm_access")
   protected Access realmAccess;

   @JsonProperty("resource_access")
   protected Map<String, Access> resourceAccess = new HashMap<String, Access>();

   public Map<String, Access> getResourceAccess()
   {
      return resourceAccess;
   }

   /**
    * Does the realm require verifying the caller?
    *
    * @return
    */
   @JsonIgnore
   public boolean isVerifyCaller()
   {
      if (getRealmAccess() != null && getRealmAccess().getVerifyCaller() != null) return getRealmAccess().getVerifyCaller().booleanValue();
      return false;
   }

   /**
    * Does the resource override the requirement of verifying the caller?
    *
    * @param resource
    * @return
    */
   @JsonIgnore
   public boolean isVerifyCaller(String resource)
   {
      Access access = getResourceAccess(resource);
      if (access != null && access.getVerifyCaller() != null) return access.getVerifyCaller().booleanValue();
      return false;
   }

   @JsonIgnore
   public Access getResourceAccess(String resource)
   {
      return resourceAccess.get(resource);
   }

   public Access addAccess(String service)
   {
      Access token = new Access();
      resourceAccess.put(service, token);
      return token;
   }

   @Override
   public SkeletonKeyToken id(String id)
   {
      return (SkeletonKeyToken)super.id(id);
   }

   @Override
   public SkeletonKeyToken expiration(long expiration)
   {
      return (SkeletonKeyToken)super.expiration(expiration);
   }

   @Override
   public SkeletonKeyToken notBefore(long notBefore)
   {
      return (SkeletonKeyToken)super.notBefore(notBefore);
   }

   @Override
   public SkeletonKeyToken issuedAt(long issuedAt)
   {
      return (SkeletonKeyToken)super.issuedAt(issuedAt);
   }

   @Override
   public SkeletonKeyToken issuer(String issuer)
   {
      return (SkeletonKeyToken)super.issuer(issuer);
   }

   @Override
   public SkeletonKeyToken audience(String audience)
   {
      return (SkeletonKeyToken)super.audience(audience);
   }

   @Override
   public SkeletonKeyToken principal(String principal)
   {
      return (SkeletonKeyToken)super.principal(principal);
   }

   @Override
   public SkeletonKeyToken type(String type)
   {
      return (SkeletonKeyToken)super.type(type);
   }

   public Access getRealmAccess()
   {
      return realmAccess;
   }

   public void setRealmAccess(Access realmAccess)
   {
      this.realmAccess = realmAccess;
   }

   public Set<String> getTrustedCertificates()
   {
      return trustedCertificates;
   }

   public void setTrustedCertificates(Set<String> trustedCertificates)
   {
      this.trustedCertificates = trustedCertificates;
   }

   /**
    * OAuth client the token was issued for.
    *
    * @return
    */
   public String getIssuedFor()
   {
      return issuedFor;
   }

   public SkeletonKeyToken issuedFor(String issuedFor)
   {
      this.issuedFor = issuedFor;
      return this;
   }
}
