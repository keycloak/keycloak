package org.keycloak.services.model;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.annotation.IdentityProperty;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeRelationship extends AbstractAttributedType implements Relationship
{
   private static final long serialVersionUID = 1L;

   public static final RelationshipQueryParameter CLIENT = new RelationshipQueryParameter() {

      @Override
      public String getName() {
         return "client";
      }
   };

   protected Agent client;
   protected Role scope;

   @IdentityProperty
   public Agent getClient()
   {
      return client;
   }

   public void setClient(Agent client)
   {
      this.client = client;
   }

   @IdentityProperty
   public Role getScope()
   {
      return scope;
   }

   public void setScope(Role scope)
   {
      this.scope = scope;
   }
}
