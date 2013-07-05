package org.keycloak.services.models.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.annotation.IdentityProperty;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminRelationship extends AbstractAttributedType implements Relationship
{
   private static final long serialVersionUID = 1L;

   public static final RelationshipQueryParameter REALM = new RelationshipQueryParameter() {

      @Override
      public String getName() {
         return "realm";
      }
   };

   public static final RelationshipQueryParameter ADMIN = new RelationshipQueryParameter() {

      @Override
      public String getName() {
         return "admin";
      }
   };

   protected Realm realm;
   protected Agent admin;

   @IdentityProperty
   public Realm getRealm()
   {
      return realm;
   }

   public void setRealm(Realm realm)
   {
      this.realm = realm;
   }

   @IdentityProperty
   public Agent getAdmin()
   {
      return admin;
   }

   public void setAdmin(Agent admin)
   {
      this.admin = admin;
   }
}
