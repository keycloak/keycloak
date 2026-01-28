package org.keycloak.testsuite.broker;

import java.util.Collections;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Element;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_LOGIN;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_PASSWORD;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

import static org.hamcrest.MatcherAssert.assertThat;

public class KcSamlBrokerDestinationTest extends AbstractBrokerTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

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

                    String consumerRealmId = realmsResouce().realm(bc.consumerRealmName()).toRepresentation().getId();
                    String expectedError = Errors.INVALID_SAML_RESPONSE;

                    events.expect(EventType.IDENTITY_PROVIDER_RESPONSE)
                            .clearDetails()
                            .session((String) null)
                            .realm(consumerRealmId)
                            .user((String) null)
                            .client((String) null)
                            .error(expectedError)
                            .detail("reason", Errors.MISSING_REQUIRED_DESTINATION)
                            .assertEvent();
                    events.assertEmpty();
                });
    }
}