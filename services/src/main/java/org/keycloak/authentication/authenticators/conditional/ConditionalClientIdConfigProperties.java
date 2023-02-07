package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.keycloak.authentication.authenticators.conditional.ConditionalClientIdConfig.CONDITIONAL_CLIENT_IDS;
import static org.keycloak.authentication.authenticators.conditional.ConditionalClientIdConfig.CONF_NEGATE;
import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.MULTIVALUED_STRING_TYPE;

final class ConditionalClientIdConfigProperties {

    private static final ProviderConfigProperty CLIENT_IDS_PROPERTY = new ProviderConfigProperty(
        CONDITIONAL_CLIENT_IDS,
        "Client Ids",
        "Client ids that match the condition.",
        MULTIVALUED_STRING_TYPE,
        emptyList(),
        false);

    private static final ProviderConfigProperty NEGATE_PROPERTY = new ProviderConfigProperty(
        CONF_NEGATE,
        "Negate output",
        "Apply a NOT to the check result. When this is true, then the condition will evaluate to true just if clientId is NOT in the specified list of client ids. When this is false, the condition will evaluate to true just if the client id is in the list of specified client ids.",
        BOOLEAN_TYPE,
        false,
        false);


    static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
        .property(CLIENT_IDS_PROPERTY)
        .property(NEGATE_PROPERTY)
        .build();

}
