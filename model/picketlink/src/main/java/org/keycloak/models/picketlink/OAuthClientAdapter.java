package org.keycloak.models.picketlink;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.picketlink.relationships.OAuthClientRelationship;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.RelationshipManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter implements OAuthClientModel {
    protected OAuthClientRelationship delegate;
    protected IdentityManager idm;
    protected RelationshipManager relationshipManager;

    public OAuthClientAdapter(OAuthClientRelationship delegate, IdentityManager idm, RelationshipManager relationshipManager) {
        this.delegate = delegate;
        this.idm = idm;
        this.relationshipManager = relationshipManager;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public UserModel getOAuthAgent() {
       return new UserAdapter(delegate.getOauthAgent(), idm);
    }

    @Override
    public String getBaseUrl() {
        return delegate.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String base) {
        delegate.setBaseUrl(base);
        relationshipManager.update(delegate);
    }
}
