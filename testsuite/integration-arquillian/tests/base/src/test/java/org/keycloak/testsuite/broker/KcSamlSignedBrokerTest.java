package org.keycloak.testsuite.broker;

import org.junit.Ignore;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.broker.BrokerTestConstants.*;

@Ignore
public class KcSamlSignedBrokerTest extends KcSamlBrokerTest {

    @Override
    protected RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = super.createProviderRealm();

        realm.setPublicKey(REALM_PUBLIC_KEY);
        realm.setPrivateKey(REALM_PRIVATE_KEY);

        return realm;
    }

    @Override
    protected RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = super.createConsumerRealm();

        realm.setPublicKey(REALM_PUBLIC_KEY);
        realm.setPrivateKey(REALM_PRIVATE_KEY);

        return realm;
    }

    @Override
    protected List<ClientRepresentation> createProviderClients() {
        List<ClientRepresentation> clientRepresentationList = super.createProviderClients();

        for (ClientRepresentation client : clientRepresentationList) {
            client.setClientAuthenticatorType("client-secret");
            client.setSurrogateAuthRequired(false);

            Map<String, String> attributes = client.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
                client.setAttributes(attributes);
            }

            attributes.put("saml.assertion.signature", "true");
            attributes.put("saml.server.signature", "true");
            attributes.put("saml.client.signature", "true");
            attributes.put("saml.signature.algorithm", "RSA_SHA256");
            attributes.put("saml.signing.private.key", IDP_SAML_SIGN_KEY);
            attributes.put("saml.signing.certificate", IDP_SAML_SIGN_CERT);
        }

        return clientRepresentationList;
    }

    @Override
    protected IdentityProviderRepresentation setUpIdentityProvider() {
        IdentityProviderRepresentation result = super.setUpIdentityProvider();

        Map<String, String> config = result.getConfig();

        config.put("validateSignature", "true");
        config.put("wantAuthnRequestsSigned", "true");
        config.put("signingCertificate", IDP_SAML_SIGN_CERT);

        return result;
    }
}
