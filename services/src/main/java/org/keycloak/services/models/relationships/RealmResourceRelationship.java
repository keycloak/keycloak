package org.keycloak.services.models.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.annotation.IdentityProperty;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmResourceRelationship extends AbstractAttributedType implements Relationship
{
   private static final long serialVersionUID = 1L;

   public static final RelationshipQueryParameter REALM_AGENT = new RelationshipQueryParameter() {

      @Override
      public String getName() {
         return "realmAgent";
      }
   };

   public static final RelationshipQueryParameter RESOURCE_AGENT = new RelationshipQueryParameter() {

      @Override
      public String getName() {
         return "resourceAgent";
      }
   };

   protected Agent realmAgent;
   protected Agent resourceAgent;

   @IdentityProperty
   public Agent getRealmAgent()
   {
      return realmAgent;
   }

   public void setRealmAgent(Agent realmAgent)
   {
      this.realmAgent = realmAgent;
   }

   @IdentityProperty
   public Agent getResourceAgent()
   {
      return resourceAgent;
   }

   public void setResourceAgent(Agent resourceAgent)
   {
      this.resourceAgent = resourceAgent;
   }
}
