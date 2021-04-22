package org.keycloak.testsuite.broker;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.ReverseProxy;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_LOGIN;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_PASSWORD;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public final class KcSamlBrokerFrontendUrlTest extends AbstractBrokerTest {

    @Rule
    public ReverseProxy proxy = new ReverseProxy();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration() {
            @Override 
            public RealmRepresentation createConsumerRealm() {
                RealmRepresentation realm = super.createConsumerRealm();

                Map<String, String> attributes = new HashMap<>();

                attributes.put("frontendUrl", proxy.getUrl());

                realm.setAttributes(attributes);
                
                return realm;
            }

            @Override 
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> clients = super.createProviderClients();

                List<String> redirectUris = new ArrayList<>();

                redirectUris.add(proxy.getUrl() + "/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint/*");

                clients.get(0).setRedirectUris(redirectUris);
                
                return clients;
            }

            @Override
            public String getIDPClientIdInProviderRealm() {
                return proxy.getUrl() + "/realms/" + consumerRealmName();
            }
        };
    }
    
    private SamlClientBuilder clientBuilderTrustingAllCertificates() {
        return new SamlClientBuilder() {
            @Override
            protected SamlClient createSamlClient() {
                return new SamlClient() {
                    @Override
                    protected HttpClientBuilder createHttpClientBuilderInstance() {
                        try {
                            return super.createHttpClientBuilderInstance()
                                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };
    }

    @Test
    @Override
    public void testLogInAsUserInIDP() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");

        driver.navigate().to(proxy.getUrl() + "/realms/consumer/account");
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        log.debug("Logging in");

        // make sure the frontend url is used to build the redirect uri when redirecting to the broker
        try {
            assertThat(driver.getCurrentUrl(), containsString("client_id=" + URLEncoder.encode(proxy.getUrl(), "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        waitForPage(driver, "account management", true);
        accountUpdateProfilePage.assertCurrent();
    }

    @Test
    public void testFrontendUrlInDestinationExpected() throws URISyntaxException {
        SAMLDocumentHolder samlResponse = clientBuilderTrustingAllCertificates()
                .idpInitiatedLogin(new URI(proxy.getUrl() + "/realms/" + bc.consumerRealmName() + "/protocol/saml"), "sales-post").build()
                // Request login via kc-saml-idp
                .login().idp(IDP_SAML_ALIAS).build()

                .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

                // Login in provider realm
                .login().user(USER_LOGIN, USER_PASSWORD).build()

                // Send the response to the consumer realm
                .processSamlResponse(SamlClient.Binding.POST)
                    .transformObject(saml2Object -> {
                        assertThat(saml2Object, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                        ResponseType response = (ResponseType) saml2Object;

                        assertThat(response.getDestination(), startsWith(proxy.getUrl()));

                        return saml2Object;
                    })
                .build()

                // Create account in comsumer realm
                .updateProfile().username(USER_LOGIN).email(USER_EMAIL).firstName("Firstname").lastName("Lastname").build()
                .followOneRedirect()

                // Obtain the response sent to the app
                .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void testKeycloakRejectsRealUrlWhenFrontendUrlConfigured() throws URISyntaxException {
        clientBuilderTrustingAllCertificates()
                .idpInitiatedLogin(new URI(proxy.getUrl() + "/realms/" + bc.consumerRealmName() + "/protocol/saml"), "sales-post").build()
                // Request login via kc-saml-idp
                .login().idp(IDP_SAML_ALIAS).build()

                .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

                // Login in provider realm
                .login().user(USER_LOGIN, USER_PASSWORD).build()

                // Send the response to the consumer realm
                .processSamlResponse(SamlClient.Binding.POST)
                .transformObject(saml2Object -> {
                    assertThat(saml2Object, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                    ResponseType response = (ResponseType) saml2Object;

                    assertThat(response.getDestination(), startsWith(proxy.getUrl()));

                    response.setDestination(getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint");
                    return saml2Object;
                })
                .build()

                // Obtain the response sent to the app
                .execute(response -> {
                    assertThat(response, Matchers.statusCodeIsHC(Response.Status.BAD_REQUEST));
                    String consumerRealmId = realmsResouce().realm(bc.consumerRealmName()).toRepresentation().getId();
                    events.expect(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                            .clearDetails()
                            .session((String) null)
                            .realm(consumerRealmId)
                            .user((String) null)
                            .client((String) null)
                            .error(Errors.INVALID_SAML_RESPONSE)
                            .detail("reason", Errors.INVALID_DESTINATION)
                            .assertEvent();
                    events.assertEmpty();
                });
    }

    @Ignore
    @Test
    @Override
    public void loginWithExistingUser() {
        // no-op
    }
}
