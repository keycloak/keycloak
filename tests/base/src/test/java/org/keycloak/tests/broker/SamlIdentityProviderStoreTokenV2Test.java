package org.keycloak.tests.broker;

import java.security.PublicKey;
import java.util.Base64;

import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.w3c.dom.Document;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = SamlIdentityProviderStoreTokenV2Test.IdentityBrokeringAPIV2ServerConfig.class)
public class SamlIdentityProviderStoreTokenV2Test implements InterfaceIdentityProviderStoreTokenV2Test, InterfaceSamlIdentityProviderStoreTokenTest {

    @InjectRealm(config = IdpRealmConfig.class)
    protected ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectOAuthClient(config = ExternalClientConfig.class)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Override
    public ManagedRealm getRealm() {
        return realm;
    }

    @Override
    public ManagedRealm getExternalRealm() {
        return externalRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public RunOnServerClient getRunOnServer() {
        return runOnServer;
    }

    @Override
    public TimeOffSet getTimeOffSet() {
        return timeOffSet;
    }

    @Override
    public void checkSuccessfulTokenResponse(AbstractHttpResponse response) {
        try {
            Assertions.assertInstanceOf(AccessTokenResponse.class, response);
            AccessTokenResponse externalTokens = (AccessTokenResponse) response;
            Assertions.assertEquals(TokenUtil.TOKEN_TYPE_BEARER, externalTokens.getTokenType());
            Assertions.assertNotNull(externalTokens.getAccessToken());
            Document assertion = DocumentUtil.getDocument(
                    new String(Base64.getUrlDecoder().decode(externalTokens.getAccessToken()), GeneralConstants.SAML_CHARSET));
            Assertions.assertEquals(JBossSAMLConstants.ASSERTION.getNsUri().get(), assertion.getDocumentElement().getNamespaceURI());
            Assertions.assertEquals(JBossSAMLConstants.ASSERTION.get(), assertion.getDocumentElement().getLocalName());
            Assertions.assertTrue(AssertionUtil.isSignedElement(assertion.getDocumentElement()));

            KeysMetadataRepresentation keysMetadata = getExternalRealm().admin().keys().getKeyMetadata();
            String kid = keysMetadata.getActive().get("RS256");
            KeysMetadataRepresentation.KeyMetadataRepresentation keyMetadata = keysMetadata.getKeys().stream()
                    .filter(k -> kid.equals(k.getKid())).findAny().orElse(null);
            PublicKey realmPubKey = KeycloakModelUtils.getPublicKey(keyMetadata.getPublicKey());

            SamlProtocolUtils.verifyDocumentSignature(assertion, new HardcodedKeyLocator(realmPubKey));
        } catch (VerificationException | ConfigurationException | ParsingException | ProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static class IdentityBrokeringAPIV2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_BROKERING_API_V2);
        }
    }
}
