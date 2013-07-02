package org.keycloak;

import java.security.Principal;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyPrincipal implements Principal
{
   protected String name;
   protected String surrogate;

   public SkeletonKeyPrincipal(String name, String surrogate)
   {
      this.name = name;
      this.surrogate = surrogate;
   }

   @Override
   public String getName()
   {
      return name;
   }

   public String getSurrogate()
   {
      return surrogate;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SkeletonKeyPrincipal that = (SkeletonKeyPrincipal) o;

      if (!name.equals(that.name)) return false;
      if (surrogate != null ? !surrogate.equals(that.surrogate) : that.surrogate != null) return false;

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = name.hashCode();
      result = 31 * result + (surrogate != null ? surrogate.hashCode() : 0);
      return result;
   }

   @Override
   public String toString()
   {
      return name;
   }
}
