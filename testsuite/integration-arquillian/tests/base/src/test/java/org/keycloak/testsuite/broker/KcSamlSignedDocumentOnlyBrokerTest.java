package org.keycloak.testsuite.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_SIGN_CERT;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_SIGN_KEY;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PRIVATE_KEY;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PUBLIC_KEY;

public class KcSamlSignedDocumentOnlyBrokerTest extends AbstractBrokerTest {

    public static class KcSamlSignedBrokerConfiguration extends KcSamlBrokerConfiguration {

        @Override
        public RealmRepresentation createProviderRealm() {
            RealmRepresentation realm = super.createProviderRealm();

            realm.setPublicKey(REALM_PUBLIC_KEY);
            realm.setPrivateKey(REALM_PRIVATE_KEY);

            return realm;
        }

        @Override
        public RealmRepresentation createConsumerRealm() {
            RealmRepresentation realm = super.createConsumerRealm();

            realm.setPublicKey(REALM_PUBLIC_KEY);
            realm.setPrivateKey(REALM_PRIVATE_KEY);

            return realm;
        }

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientRepresentationList = super.createProviderClients();

            for (ClientRepresentation client : clientRepresentationList) {
                client.setClientAuthenticatorType("client-secret");
                client.setSurrogateAuthRequired(false);

                Map<String, String> attributes = client.getAttributes();
                if (attributes == null) {
                    attributes = new HashMap<>();
                    client.setAttributes(attributes);
                }

                attributes.put("saml.assertion.signature", "false");
                attributes.put("saml.server.signature", "true");
                attributes.put("saml.client.signature", "true");
                attributes.put("saml.signature.algorithm", "RSA_SHA256");
                attributes.put("saml.signing.private.key", IDP_SAML_SIGN_KEY);
                attributes.put("saml.signing.certificate", IDP_SAML_SIGN_CERT);
            }

            return clientRepresentationList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation result = super.setUpIdentityProvider(syncMode);

            Map<String, String> config = result.getConfig();

            config.put("validateSignature", "true");
            config.put("wantAssertionsSigned", "false");
            config.put("wantAuthnRequestsSigned", "true");
            config.put("signingCertificate", IDP_SAML_SIGN_CERT);

            return result;
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlSignedBrokerConfiguration.INSTANCE;
    }

}
