package org.keycloak.testsuite.broker;

import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

public interface NestedBrokerConfiguration extends BrokerConfiguration {

    RealmRepresentation createSubConsumerRealm();

    String subConsumerRealmName();

    IdentityProviderRepresentation setUpConsumerIdentityProvider();

    String getSubConsumerIDPDisplayName();
}
