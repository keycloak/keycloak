package org.keycloak.models;

import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface OAuthClientModel {
    String getId();

    UserModel getOAuthAgent();

    String getBaseUrl();

    void setBaseUrl(String base);
}
