package org.keycloak.testsuite.saml;

import java.io.Closeable;
import java.util.List;
import java.util.UUID;

import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.testsuite.AbstractConcurrencyTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;


/**
 * @author mhajas
 */
public class SamlRelayStateTest extends AbstractSamlTest {

    private static final String RELAY_STATE = "/importantRelayState";

    @Test
    public void testRelayStateDoesNotRetainBetweenTwoRequestsPost() throws Exception {
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .relayState(RELAY_STATE)
                    .build()

                .login().user(bburkeUser).build()
                .assertSamlRelayState(SamlClient.Binding.POST,
                        relayState -> assertThat(relayState, is(equalTo(RELAY_STATE))))

                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .build()
                .followOneRedirect()
                .assertSamlRelayState(SamlClient.Binding.POST,
                        relayState -> assertThat(relayState, is(nullValue())))
                .execute();
    }

    @Test
    public void testRelayStateDoesNotRetainBetweenTwoRequestsRedirect() throws Exception {
        String url = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0)
                .getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
        try (Closeable c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, null)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, url)
                .update()) {
            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.REDIRECT)
                        .relayState(RELAY_STATE)
                        .build()

                    .login().user(bburkeUser).build()
                    .assertSamlRelayState(SamlClient.Binding.REDIRECT,
                            relayState -> assertThat(relayState, is(equalTo(RELAY_STATE))))

                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.REDIRECT)
                        .build()
                    .assertSamlRelayState(SamlClient.Binding.REDIRECT,
                            relayState -> assertThat(relayState, is(nullValue())))
                    .execute();
        }
    }

    @Test
    public void testRelayStateDoesNotRetainBetweenTwoRequestsIdpInitiatedPost() throws Exception {
        new SamlClientBuilder()
                .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post")
                    .relayState(RELAY_STATE)
                    .build()
                .login().user(bburkeUser).build()

                .assertSamlRelayState(SamlClient.Binding.POST,
                        relayState -> assertThat(relayState, is(equalTo(RELAY_STATE))))

                .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post")
                    .build()

                .assertSamlRelayState(SamlClient.Binding.POST,
                        relayState -> assertThat(relayState, is(nullValue())))
                .execute();
    }

    @Test
    public void testRelayStateDoesNotRetainBetweenTwoRequestsIdpInitiatedRedirect() throws Exception {
        String url = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0)
                .getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
        try (Closeable c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, null)
                .setAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, url)
                .update()) {
            new SamlClientBuilder()
                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post")
                        .relayState(RELAY_STATE)
                        .build()
                    .login().user(bburkeUser).build()

                    .assertSamlRelayState(SamlClient.Binding.REDIRECT,
                            relayState -> assertThat(relayState, is(equalTo(RELAY_STATE))))

                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post")
                        .build()

                    .assertSamlRelayState(SamlClient.Binding.REDIRECT,
                            relayState -> assertThat(relayState, is(nullValue())))
                    .execute();
        }
    }

    @Test
    public void testRelayStateForSameAuthSession() throws Exception {
        new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .relayState(RELAY_STATE)
                    .build()

                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .build()

                .login().user(bburkeUser).build()
                .assertSamlRelayState(SamlClient.Binding.POST,
                        relayState -> assertThat(relayState, is(nullValue())))
                .execute();
    }

    @Test
    public void testRelayStateForSameAuthSessionIDPInitiated() throws Exception {
        new SamlClientBuilder()
                .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post")
                    .relayState(RELAY_STATE)
                    .build()

                .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                .login().user(bburkeUser).build()
                .assertSamlRelayState(SamlClient.Binding.POST,
                        relayState -> assertThat(relayState, is(nullValue())))
                .execute();
    }

    @Test
    @Ignore("KEYCLOAK-5179")
    public void relayStateConcurrencyTest() throws Exception {
        ThreadLocal<UUID> tl = new ThreadLocal<>();

        List<SamlClient.Step> steps = new SamlClientBuilder()
                .addStep(() -> tl.set(UUID.randomUUID()))
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                    .relayState(() -> tl.get().toString())
                    .build()

                .login().user(bburkeUser).build()
                .assertSamlRelayState(SamlClient.Binding.POST, relayState -> {
                    assertThat(relayState, is(notNullValue()));
                    assertThat(relayState, is(equalTo(tl.get().toString())));
                })
                .getSteps();

        SamlClient client = new SamlClient();
        client.execute(steps);
        steps.remove(2); // removing login as it should not be necessary anymore

        AbstractConcurrencyTest.run(2, 10, this, (threadIndex, keycloak, realm) -> {
            client.execute(steps);
        });
    }
}
