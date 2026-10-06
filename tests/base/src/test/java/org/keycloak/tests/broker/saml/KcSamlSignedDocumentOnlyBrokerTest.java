package org.keycloak.tests.broker.saml;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest
public class KcSamlSignedDocumentOnlyBrokerTest extends AbstractKcSamlBrokerTest {

    @BeforeEach
    void configureSignedDocumentOnly() {
        String providerSigningCert = activeRs256SigCert(providerRealm);
        String consumerSigningCert = activeRs256SigCert(consumerRealm);

        // AbstractKcSamlBrokerTest.configureSamlBrokerEndpoints() (superclass @BeforeEach, runs before
        // this one) already renamed the provider client from the seeded placeholder entity id to the
        // consumer's actual base URL, so that's the id to look up here, not the placeholder.
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
        // Require and validate the consumer's signature on incoming AuthnRequests.
        client.getAttributes().put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, consumerSigningCert);
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        idp.getConfig().put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "false");
        idp.getConfig().put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerSigningCert);
        // Sign the AuthnRequests sent to the provider so it can validate them.
        idp.getConfig().put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }

    private static String activeRs256SigCert(ManagedRealm realm) {
        return realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> k.getCertificate() != null && KeyUse.SIG.equals(k.getUse())
                        && Algorithm.RS256.equals(k.getAlgorithm()))
                .map(KeyMetadataRepresentation::getCertificate)
                .findFirst().orElseThrow();
    }
}
