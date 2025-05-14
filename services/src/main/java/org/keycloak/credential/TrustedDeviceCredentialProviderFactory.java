package org.keycloak.credential;

import org.keycloak.models.KeycloakSession;

public class TrustedDeviceCredentialProviderFactory implements CredentialProviderFactory<TrustedDeviceCredentialProvider> {
    public static final String PROVIDER_ID = "keycloak-trusted-device";

    @Override
    public TrustedDeviceCredentialProvider create(KeycloakSession session) {
        return new TrustedDeviceCredentialProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
