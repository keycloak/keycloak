package org.keycloak.authentication.authenticators.browser;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.keycloak.authentication.authenticators.browser.ForceLoAAuthenticatorConfig.MIN_LOA;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

final class ForceLoAAuthenticatorConfigProperties {

    private static final ProviderConfigProperty MIN_LOA_PROPERTY = new ProviderConfigProperty(
        MIN_LOA,
        "Minimum Level of Authenticaton (LoA)",
        "The minimum level of authentication enforced by this authenticator. If a client request a LoA greater than this value, the requested LoA will be used. Otherwise, this configured value will be used.",
        STRING_TYPE,
        emptyList(),
        false);

    static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
        .property(MIN_LOA_PROPERTY)
        .build();

}
