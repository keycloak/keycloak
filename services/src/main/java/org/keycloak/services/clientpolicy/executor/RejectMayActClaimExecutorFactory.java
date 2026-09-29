package org.keycloak.services.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 *
 * @author rmartinc
 */
public class RejectMayActClaimExecutorFactory implements ClientPolicyExecutorProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "may-act";

    public static final String REJECT_ANY_MAY_ACT = "reject-any";
    public static final String AVOID_PERMISSION_CHECK = "avoid-permission-check";
    public static final String AVOID_CONSENT_CHECK = "avoid-consent-check";

    @Override
    public String getHelpText() {
        return """
               Checks the 'may_act' claim in token responses and rejects tokens with missing or invalid
               'may_act' values based on issuer, subject, permission, or consent validation
               """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()

                .property()
                .name(REJECT_ANY_MAY_ACT)
                .label("Reject any may_act claim")
                .helpText("If ON, any token with a may_act claim is rejected. If OFF, the may_act claim is checked using the other conditions")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()

                .property()
                .name(AVOID_PERMISSION_CHECK)
                .label("Avoid permission check")
                .helpText("If ON, permission check is not executed. If OFF, the subject in the may_act claim is checked to have the correct delegation permission.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()

                .property()
                .name(AVOID_CONSENT_CHECK)
                .label("Avoid consent check")
                .helpText("If ON, consent check is not executed. If OFF, the expected user or client scope is checked to be consented by the final user.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()

                .build();
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new RejectMayActClaimExecutor(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Feature.TOKEN_EXCHANGE_DELEGATION);
    }
}
