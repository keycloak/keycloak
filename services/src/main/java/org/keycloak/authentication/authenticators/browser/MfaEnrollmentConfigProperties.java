package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

import static javax.management.remote.rmi.RMIConnectorServer.CREDENTIAL_TYPES;
import static org.keycloak.authentication.authenticators.browser.MfaEnrollmentConfig.REQUIRED_ACTIONS;
import static org.keycloak.provider.ProviderConfigProperty.MULTIVALUED_STRING_TYPE;

final class MfaEnrollmentConfigProperties {

    private static final ProviderConfigProperty REQUIRED_ACTIONS_PROPERTY = new ProviderConfigProperty(
        REQUIRED_ACTIONS,
        "Required actions",
        "Required actions the user can choose from.",
        MULTIVALUED_STRING_TYPE,
        String.join(Constants.CFG_DELIMITER,
            UserModel.RequiredAction.CONFIGURE_TOTP.name(),
            WebAuthnRegisterFactory.PROVIDER_ID,
            WebAuthnPasswordlessRegisterFactory.PROVIDER_ID
        ),
        false);

    private static final ProviderConfigProperty CREDENTIAL_TYPES_PROPERTY = new ProviderConfigProperty(
            CREDENTIAL_TYPES,
            "Credential types",
            "Credential types for multi factor.",
            MULTIVALUED_STRING_TYPE,
            String.join(Constants.CFG_DELIMITER,
                    OTPCredentialModel.TYPE,
                    WebAuthnCredentialModel.TYPE_TWOFACTOR,
                    WebAuthnCredentialModel.TYPE_PASSWORDLESS
            ),
            false);

    static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
        .property(REQUIRED_ACTIONS_PROPERTY)
        .property(CREDENTIAL_TYPES_PROPERTY)
        .build();

}
