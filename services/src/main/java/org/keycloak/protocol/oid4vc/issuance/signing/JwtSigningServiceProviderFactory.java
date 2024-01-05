package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class JwtSigningServiceProviderFactory extends VCSigningServiceProviderFactory {

    public static final Format SUPPORTED_FORMAT = Format.JWT_VC;
    private static final String HELP_TEXT = "Issues JWT-VCs following the specification of https://identity.foundation/jwt-vc-presentation-profile/.";

    @Override
    public VerifiableCredentialsSigningService create(KeycloakSession session, ComponentModel model) {
        var keyId = model.get(SigningProperties.KEY_ID.getKey());
        var algorithmType = model.get(SigningProperties.ALGORITHM_TYPE.getKey());

        return new JwtSigningService(session, keyId, CLOCK, algorithmType);
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return VCSigningServiceProviderFactory.configurationBuilder()
                .property(SigningProperties.ALGORITHM_TYPE.asConfigProperty())
                .build();
    }

    @Override
    public String getId() {
        return SUPPORTED_FORMAT.toString();
    }

    @Override
    void validateSpecificConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkRequired(SigningProperties.ALGORITHM_TYPE.asConfigProperty());
    }

    @Override
    public Format supportedFormat() {
        return SUPPORTED_FORMAT;
    }
}
