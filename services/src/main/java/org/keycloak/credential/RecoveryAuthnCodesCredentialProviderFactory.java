package org.keycloak.credential;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;

public class RecoveryAuthnCodesCredentialProviderFactory
        implements CredentialProviderFactory<RecoveryAuthnCodesCredentialProvider>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "keycloak-recovery-authn-codes";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public RecoveryAuthnCodesCredentialProvider create(KeycloakSession session) {
        return new RecoveryAuthnCodesCredentialProvider(session);
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.RECOVERY_CODES);
    }
}
