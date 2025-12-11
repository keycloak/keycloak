package org.keycloak.testsuite.broker;

import java.util.List;

import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 *
 * @author hmlnarik
 */
public interface BrokerConfiguration {

    /**
     * @return Representation of the realm at the identity provider side.
     */
    RealmRepresentation createProviderRealm();

    /**
     * @return Representation of the realm at the broker side.
     */
    RealmRepresentation createConsumerRealm();

    List<ClientRepresentation> createProviderClients();

    List<ClientRepresentation> createConsumerClients();

    /**
     * @return Representation of the identity provider for declaration in the broker
     */
    default IdentityProviderRepresentation setUpIdentityProvider() {
        return setUpIdentityProvider(IdentityProviderSyncMode.IMPORT);
    }

    /**
     * @return Representation of the identity provider for declaration in the broker
     */
    IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode force);

    /**
     * @return Name of realm containing identity provider. Must be consistent with {@link #createProviderRealm()}
     */
    String providerRealmName();

    /**
     * @return Realm name of the broker. Must be consistent with {@link #createConsumerRealm()}
     */
    String consumerRealmName();

    /**
     * @return Client ID of the identity provider as set in provider realm.
     */
    String getIDPClientIdInProviderRealm();

    /**
     * @return User login name of the brokered user
     */
    String getUserLogin();

    /**
     * @return Password of the brokered user
     */
    String getUserPassword();

    /**
     * @return E-mail of the brokered user
     */
    String getUserEmail();

    /**
     * @return Alias of the identity provider as defined in the broker realm
     */
    String getIDPAlias();
}
