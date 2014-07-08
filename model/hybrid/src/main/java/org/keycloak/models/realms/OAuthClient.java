package org.keycloak.models.realms;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface OAuthClient extends Client {

    void setClientId(String id);

}
