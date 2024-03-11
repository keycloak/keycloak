package org.keycloak.protocol.oid4vc;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.ClientRegistrationProviderFactory;

import java.util.List;

/**
 * Implementation of the {@link ClientRegistrationProviderFactory} to integrate the OID4VC protocols with
 * Keycloaks client-registration.
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCClientRegistrationProviderFactory implements ClientRegistrationProviderFactory, OID4VCEnvironmentProviderFactory {

    public static final String PROTOCOL_ID = "oid4vc";

    @Override
    public ClientRegistrationProvider create(KeycloakSession session) {
        return new OID4VCClientRegistrationProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // no config required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to do post init
    }

    @Override
    public void close() {
        // no resources to close
    }

    @Override
    public String getId() {
        return OID4VCClientRegistrationProviderFactory.PROTOCOL_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigProperty issuerDid = new ProviderConfigProperty();
        issuerDid.setName("issuer_did");
        issuerDid.setHelpText("DID to be used for issuing verifiable credentials.");
        issuerDid.setType(ProviderConfigProperty.STRING_TYPE);
        issuerDid.setLabel("Issuer DID");

        ProviderConfigProperty keyPath = new ProviderConfigProperty();
        keyPath.setName("key_path");
        keyPath.setHelpText("Path to read the signing key from.");
        keyPath.setType(ProviderConfigProperty.STRING_TYPE);
        keyPath.setLabel("Key Path");

        return List.of(issuerDid, keyPath);
    }
}