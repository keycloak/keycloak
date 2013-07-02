package org.keycloak.services.model;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.model.annotation.IdentityProperty;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RequiredCredentialRelationship extends AbstractAttributedType implements Relationship
{
   private static final long serialVersionUID = 1L;

   public static final RelationshipQueryParameter REALM_AGENT = new RelationshipQueryParameter() {

      @Override
      public String getName() {
         return "realmAgent";
      }
   };


   protected Agent realmAgent;
   protected String credentialType;
   protected boolean input;
   protected boolean secret;

   public RequiredCredentialRelationship()
   {
   }

   @IdentityProperty
   public Agent getRealmAgent()
   {
      return realmAgent;
   }

   public void setRealmAgent(Agent realmAgent)
   {
      this.realmAgent = realmAgent;
   }

   @AttributeProperty
   public String getCredentialType()
   {
      return credentialType;
   }

   public void setCredentialType(String credentialType)
   {
      this.credentialType = credentialType;
   }

   @AttributeProperty
   public boolean isInput()
   {
      return input;
   }

   public void setInput(boolean input)
   {
      this.input = input;
   }

   @AttributeProperty
   public boolean isSecret()
   {
      return secret;
   }

   public void setSecret(boolean secret)
   {
      this.secret = secret;
   }
}
