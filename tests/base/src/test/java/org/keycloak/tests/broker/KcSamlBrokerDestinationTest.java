package org.keycloak.tests.broker;

import java.util.Collections;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import static org.keycloak.tests.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.tests.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.tests.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.tests.broker.BrokerTestConstants.USER_LOGIN;
import static org.keycloak.tests.broker.BrokerTestConstants.USER_PASSWORD;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public class KcSamlBrokerDestinationTest extends AbstractBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectEvents
    Events events;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration() {

            @Override
            public RealmRepresentation createProviderRealm() {
                RealmRepresentation realm = super.createProviderRealm();
                realm.setEventsListeners(Collections.singletonList("jboss-logging"));
                return realm;
            }
        };
    }

    @Test
    public void testNullDestinationInResponseShouldReturnInvalidSamlResponse() {
        getCleanup(REALM_PROV_NAME)
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                        .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.toString(true))
                        .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")    // Do not require client signature
                        .update()
                );

        new SamlClientBuilder()
                .idpInitiatedLogin(getConsumerSamlEndpoint(REALM_CONS_NAME), "sales-post").build()
                // Request login via kc-saml-idp
                .login().idp(IDP_SAML_ALIAS).build()

                .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                    .targetAttributeSamlRequest()
                    .build()

                // Login in provider realm
                .login().user(USER_LOGIN, USER_PASSWORD).build()

                // Send the response to the consumer realm
                .processSamlResponse(SamlClient.Binding.POST)
                .transformDocument(doc -> {
                    Element documentElement = doc.getDocumentElement();
                    documentElement.removeAttribute("Destination");
                })
                .build()
                .execute(response -> {

                    assertThat(response, statusCodeIsHC(Response.Status.BAD_REQUEST));
                    String expectedError = Errors.INVALID_SAML_RESPONSE;

                    EventAssertion.assertError(events.poll()).type(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                            .sessionId(null)
                            .userId(null)
                            .clientId(null)
                            .error(expectedError)
                            .details("reason", Errors.MISSING_REQUIRED_DESTINATION);
                    Assertions.assertNull(events.poll());
                });
    }
}
