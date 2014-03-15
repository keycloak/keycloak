package org.keycloak.models.picketlink.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.sample.Role;
import org.picketlink.idm.model.sample.User;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeRelationship extends AbstractAttributedType implements Relationship {
    private static final long serialVersionUID = 1L;

    public static final RelationshipQueryParameter CLIENT = new RelationshipQueryParameter() {

        @Override
        public String getName() {
            return "client";
        }
    };

    public static final RelationshipQueryParameter SCOPE = new RelationshipQueryParameter() {

        @Override
        public String getName() {
            return OAuth2Constants.SCOPE;
        }
    };


    protected User client;
    protected Role scope;

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }

    public Role getScope() {
        return scope;
    }

    public void setScope(Role scope) {
        this.scope = scope;
    }
}
