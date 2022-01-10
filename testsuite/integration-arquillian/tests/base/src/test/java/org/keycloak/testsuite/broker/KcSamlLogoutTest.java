package org.keycloak.testsuite.broker;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.SamlMessageReceiver;
import org.w3c.dom.Document;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME;
import static org.keycloak.testsuite.util.Matchers.isSamlLogoutRequest;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.Matchers.isSamlStatusResponse;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;

public class KcSamlLogoutTest extends AbstractInitializedBaseBrokerTest {

    private static final String PROVIDER_SAML_CLIENT_ID = getProviderRoot() + "/sales-post/";
    
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration(false) {
            @Override
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> superClients = super.createProviderClients();

                // Create a client in the provider realm for initiating Provider initiated logouts
                ClientRepresentation providerSamlClient = ClientBuilder.create()
                        .clientId(PROVIDER_SAML_CLIENT_ID)
                        .enabled(true)
                        .fullScopeEnabled(true)
                        .protocol(SamlProtocol.LOGIN_PROTOCOL)
                        .baseUrl(getProviderRoot() + "/sales-post")
                        .addRedirectUri(getProviderRoot() + "/sales-post/*")
                        .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
                        .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
                        .frontchannelLogout(true)
                        .build();
                
                superClients.add(providerSamlClient);
                
                return superClients;
            }
        };
    }

    @Test
    public void testProviderInitiatedLogoutCorrectlyLogsOutConsumerClients() throws Exception {
        try (SamlMessageReceiver logoutReceiver = new SamlMessageReceiver(8082);
             ClientAttributeUpdater cauConsumer = ClientAttributeUpdater.forClient(adminClient, bc.consumerRealmName(), AbstractSamlTest.SAML_CLIENT_ID_SALES_POST)
                .setFrontchannelLogout(false)
                .setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, logoutReceiver.getUrl())
                .update();
            ClientAttributeUpdater cauProvider = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setFrontchannelLogout(true)
                .update()) {

            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);

            Document doc = SAML2Request.convert(loginRep);
            
            final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
            final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

            new SamlClientBuilder()
                    // Login into SAML_CLIENT_ID_SALES_POST using provider realm as idp
                    .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()   // Request to consumer IdP
                    .login().idp(bc.getIDPAlias()).build()

                    .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                    .targetAttributeSamlRequest()
                    .build()

                    .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

                    .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP
                    .build()

                    // first-broker flow
                    .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
                    .followOneRedirect()

                    .processSamlResponse(SamlClient.Binding.POST)
                        .transformObject(saml2Object -> {
                            assertThat(saml2Object, Matchers.notNullValue());
                            assertThat(saml2Object, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                            return null;
                        })
                    .build()

                    // Login using a different client to the provider realm, should be already logged in
                    .authnRequest(getProviderSamlEndpoint(bc.providerRealmName()), PROVIDER_SAML_CLIENT_ID, PROVIDER_SAML_CLIENT_ID + "saml", POST).build()
                    .followOneRedirect()

                    // Process saml response and store reference to nameId and sessionIndex so that we can initiate logout for the session
                    .processSamlResponse(POST)
                        .transformObject(saml2Object -> {
                            assertThat(saml2Object, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                            ResponseType loginResp1 = (ResponseType) saml2Object;
                            final AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
                            assertThat(firstAssertion, Matchers.notNullValue());
                            assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

                            NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();
                            AuthnStatementType firstAssertionStatement = (AuthnStatementType) firstAssertion.getStatements().iterator().next();

                            nameIdRef.set(nameId);
                            sessionIndexRef.set(firstAssertionStatement.getSessionIndex());

                            return null;
                        })
                    .build()

                    // Send logout request to provider realm
                    .logoutRequest(getProviderSamlEndpoint(bc.providerRealmName()), PROVIDER_SAML_CLIENT_ID, POST)
                        .nameId(nameIdRef::get)
                        .sessionIndex(sessionIndexRef::get)
                    .build()

                    // Provider realm should send LogoutRequest to consumer realm
                    .processSamlResponse(POST)
                        .transformObject(saml2Object -> {
                            assertThat(saml2Object, isSamlLogoutRequest(getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint"));
                            return saml2Object;
                        })
                    .build()

                    // At this moment the AbstractSamlTest.SAML_CLIENT_ID_SALES_POST client should be contacted using backchannel logout, we will check later using logoutReceiver

                    // Consumer realm should send LogoutResponse back to provider realm
                    .executeAndTransform(response -> {
                        SAMLDocumentHolder saml2ObjectHolder = POST.extractResponse(response);
                        assertThat(saml2ObjectHolder.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                        
                        return null;
                    });
            
            // Check whether logoutReceiver contains correct LogoutRequest
            assertThat(logoutReceiver.isMessageReceived(), is(true));
            SAMLDocumentHolder message = logoutReceiver.getSamlDocumentHolder();
            assertThat(message.getSamlObject(), isSamlLogoutRequest(logoutReceiver.getUrl()));
        }
    }

    @Test // KEYCLOAK-17495
    public void testProviderInitiatedLogoutCorrectlyLogsOutConsumerClientsWhenPrincipalTypeAttribute() throws Exception {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.PRINCIPAL_TYPE, SamlPrincipalType.ATTRIBUTE.name())
            .setAttribute(SAMLIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, ATTRIBUTE_TO_MAP_NAME)
            .update();

             UserAttributeUpdater uau = UserAttributeUpdater.forUserByUsername(adminClient, bc.providerRealmName(), bc.getUserLogin())
                .setAttribute(ATTRIBUTE_TO_MAP_NAME, "masked_principal_for_consumer_idp")
                .update()
        ) {
            testProviderInitiatedLogoutCorrectlyLogsOutConsumerClients();
        }
    }
}
