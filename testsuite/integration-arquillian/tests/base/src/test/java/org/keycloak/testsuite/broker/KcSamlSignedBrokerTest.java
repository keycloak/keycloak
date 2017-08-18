package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.keycloak.testsuite.broker.BrokerTestTools.encodeUrl;

public class KcSamlSignedBrokerTest extends KcSamlBrokerTest {

    public class KcSamlSignedBrokerConfiguration extends KcSamlBrokerConfiguration {

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
        public List<ClientRepresentation> createProviderClients(SuiteContext suiteContext) {
            List<ClientRepresentation> clientRepresentationList = super.createProviderClients(suiteContext);

            String consumerCert = adminClient.realm(consumerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
            Assert.assertThat(consumerCert, Matchers.notNullValue());

            for (ClientRepresentation client : clientRepresentationList) {
                client.setClientAuthenticatorType("client-secret");
                client.setSurrogateAuthRequired(false);

                Map<String, String> attributes = client.getAttributes();
                if (attributes == null) {
                    attributes = new HashMap<>();
                    client.setAttributes(attributes);
                }

                attributes.put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
                attributes.put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
                attributes.put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true");
                attributes.put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
                attributes.put(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, consumerCert);
            }

            return clientRepresentationList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
            IdentityProviderRepresentation result = super.setUpIdentityProvider(suiteContext);

            String providerCert = adminClient.realm(providerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
            Assert.assertThat(providerCert, Matchers.notNullValue());

            Map<String, String> config = result.getConfig();

            config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
            config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerCert);

            return result;
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlSignedBrokerConfiguration();
    }

    @Test
    public void testSignedEncryptedAssertions() throws Exception {
        ClientRepresentation client = adminClient.realm(bc.providerRealmName())
          .clients()
          .findByClientId(bc.getIDPClientIdInProviderRealm(suiteContext))
          .get(0);

        final ClientResource clientResource = realmsResouce().realm(bc.providerRealmName()).clients().get(client.getId());
        Assert.assertThat(clientResource, Matchers.notNullValue());

        String providerCert = adminClient.realm(bc.providerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
        Assert.assertThat(providerCert, Matchers.notNullValue());

        String consumerCert = adminClient.realm(bc.consumerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
        Assert.assertThat(consumerCert, Matchers.notNullValue());

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true")
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
            .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false")
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerCert)
            .update();
          Closeable clientUpdater = new ClientAttributeUpdater(clientResource)
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "true")
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, consumerCert)
            .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")      // only sign assertions
            .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true")
            .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
            .update())
        {
            // Login should pass because assertion is signed.
            loginUser();

            // Logout should fail because logout response is not signed.
            driver.navigate().to(BrokerTestTools.getAuthRoot(suiteContext)
                    + "/auth/realms/" + bc.providerRealmName()
                    + "/protocol/" + "openid-connect"
                    + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(bc.providerRealmName())));

            errorPage.assertCurrent();
        }
    }
}
