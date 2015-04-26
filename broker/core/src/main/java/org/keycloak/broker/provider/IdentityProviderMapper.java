package org.keycloak.broker.provider;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface IdentityProviderMapper extends Provider, ProviderFactory<IdentityProviderMapper>,ConfiguredProvider {
    public static final String ANY_PROVIDER = "*";

    String[] getCompatibleProviders();
    String getDisplayCategory();
    String getDisplayType();

    void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context);
    void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context);


}
