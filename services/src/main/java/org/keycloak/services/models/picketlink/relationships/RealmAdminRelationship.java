package org.keycloak.services.models.picketlink.relationships;

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
public class RealmAdminRelationship extends AbstractAttributedType implements Relationship {
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

    protected String realm;
    protected Agent admin;

    @AttributeProperty
    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @IdentityProperty
    public Agent getAdmin() {
        return admin;
    }

    public void setAdmin(Agent admin) {
        this.admin = admin;
    }
}
