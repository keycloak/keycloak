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

    /**
     * Called to determine what keycloak username and email to use to process the login request from the external IDP
     * Usually used to map BrokeredIdentityContet.username or email.
     *
     * @param session
     * @param realm
     * @param mapperModel
     * @param context
     */
    void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context);

    /**
     * Called after UserModel is created for first time for this user.
     *
     * @param session
     * @param realm
     * @param user
     * @param mapperModel
     * @param context
     */
    void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context);

    /**
     * Called when this user has logged in before and has already been imported.
     *
     * @param session
     * @param realm
     * @param user
     * @param mapperModel
     * @param context
     */
    void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context);


}
