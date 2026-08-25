package org.keycloak.tests.broker.saml;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.BrokerLoginTest;
import org.keycloak.tests.broker.SamlBrokerConfigSupport;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest
public class KcSamlSignedDocumentOnlyBrokerTest implements BrokerLoginTest, SamlBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = SamlBrokerConfigSupport.SamlProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = SamlBrokerConfigSupport.SamlConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @Override
    public ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    public ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public ManagedWebDriver getWebDriver() {
        return webDriver;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public IdpReviewUserProfilePage getUpdateProfilePage() {
        return updateProfilePage;
    }

    @BeforeEach
    void configureSignedDocumentOnly() {
        String providerSigningCert = activeRs256SigCert(providerRealm);
        String consumerSigningCert = activeRs256SigCert(consumerRealm);

        String samlClientId = "http://localhost:8080/realms/" + CONSUMER_REALM;
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
