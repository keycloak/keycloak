package org.keycloak.protocol.oid4vc.issuance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.OID4VCAbstractWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.wellknown.WellKnownProvider;

import java.net.URL;

/**
 * {@link  WellKnownProvider} implementation to provide the .well-known/openid-credential-issuer endpoint, offering
 * the Credential Issuer Metadata as defined by the OID4VCI protocol
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-10.2.2}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerWellKnownProvider extends OID4VCAbstractWellKnownProvider {

    public OID4VCIssuerWellKnownProvider(KeycloakSession keycloakSession, ObjectMapper objectMapper) {
        super(keycloakSession, objectMapper);
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object getConfig() {
        return new CredentialIssuer()
                .setCredentialIssuer(getIssuer(keycloakSession.getContext()))
                .setCredentialEndpoint(getCredentialsEndpoint(keycloakSession.getContext()))
                .setCredentialsSupported(getSupportedCredentials(keycloakSession));
    }

}