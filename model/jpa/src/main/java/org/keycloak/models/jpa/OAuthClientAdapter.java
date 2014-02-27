package org.keycloak.models.jpa;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.OAuthClientEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter extends ClientAdapter implements OAuthClientModel {

    public OAuthClientAdapter(OAuthClientEntity entity) {
        super(entity);
    }
}
