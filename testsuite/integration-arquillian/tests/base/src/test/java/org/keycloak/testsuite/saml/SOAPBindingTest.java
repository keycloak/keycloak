/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.SamlClientBuilder;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.SOAP;

public class SOAPBindingTest extends AbstractSamlTest {

    @Test
    public void soapBindingAuthnWithSignatureTest() {
        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, SOAP)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                    .basicAuthentication(bburkeUser)
                .build()
                .executeAndTransform(SOAP::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(ResponseType.class));
        ResponseType rt = (ResponseType)response.getSamlObject();
        assertThat(rt.getAssertions(), not(empty()));
    }

    @Test
    public void soapBindingAuthnWithSignatureMissingDestinationTest() {
        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, SOAP)
                    .transformObject(authnRequestType -> {
                        authnRequestType.setDestination(null);
                        return authnRequestType;
                    })
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .basicAuthentication(bburkeUser)
                .build()
                .executeAndTransform(SOAP::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(ResponseType.class));
        ResponseType rt = (ResponseType)response.getSamlObject();
        assertThat(rt.getAssertions(), not(empty()));
    }

    @Test
    public void soapBindingAuthnWithoutSignatureTest() {
        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_ECP_SP)
                        .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")
                        .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                        .update()
                );

        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, SOAP)
                .basicAuthentication(bburkeUser)
                .build()
                .executeAndTransform(SOAP::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(ResponseType.class));
        ResponseType rt = (ResponseType)response.getSamlObject();
        assertThat(rt.getAssertions(), not(empty()));
    }

    @Test
    public void soapBindingAuthnWithoutSignatureMissingDestinationTest() {
        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_ECP_SP)
                        .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")
                        .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                        .update()
                );

        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, SOAP)
                    .transformObject(authnRequestType -> {
                        authnRequestType.setDestination(null);
                        return authnRequestType;
                    })
                    .basicAuthentication(bburkeUser)
                .build()
                .executeAndTransform(SOAP::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(ResponseType.class));
        ResponseType rt = (ResponseType)response.getSamlObject();
        assertThat(rt.getAssertions(), not(empty()));
    }

    @Test
    public void soapBindingLogoutWithSignature() {
        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, POST)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(bburkeUser).build()
                .processSamlResponse(POST)
                .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SOAP)
                    .nameId(nameIdRef::get)
                    .sessionIndex(sessionIndexRef::get)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .executeAndTransform(POST::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(StatusResponseType.class));
    }

    @Test
    public void soapBindingLogoutWithoutSignature() {
        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_ECP_SP)
                        .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")
                        .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                        .update()
                );

        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, POST)
                .build()
                .login().user(bburkeUser).build()
                .processSamlResponse(POST)
                    .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SOAP)
                    .nameId(nameIdRef::get)
                    .sessionIndex(sessionIndexRef::get)
                .build()
                .executeAndTransform(POST::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(StatusResponseType.class));
    }

    @Test
    public void soapBindingLogoutWithSignatureMissingDestinationTest() {
        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, POST)
                .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                .build()
                .login().user(bburkeUser).build()
                .processSamlResponse(POST)
                    .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SOAP)
                    .nameId(nameIdRef::get)
                    .sessionIndex(sessionIndexRef::get)
                    .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY)
                    .transformObject(logoutRequestType -> {
                        logoutRequestType.setDestination(null);
                        return logoutRequestType;
                    })
                .build()
                .executeAndTransform(POST::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(StatusResponseType.class));
    }

    @Test
    public void soapBindingLogoutWithoutSignatureMissingDestinationTest() {
        getCleanup()
                .addCleanup(ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_ECP_SP)
                        .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")
                        .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                        .update()
                );

        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SAML_ASSERTION_CONSUMER_URL_ECP_SP, POST)
                .build()
                .login().user(bburkeUser).build()
                .processSamlResponse(POST)
                .transformObject(this::extractNameIdAndSessionIndexAndTerminate)
                .build()
                .logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_ECP_SP, SOAP)
                .nameId(nameIdRef::get)
                .sessionIndex(sessionIndexRef::get)
                .transformObject(logoutRequestType -> {
                    logoutRequestType.setDestination(null);
                    return logoutRequestType;
                })
                .build()
                .executeAndTransform(POST::extractResponse);


        assertThat(response.getSamlObject(), instanceOf(StatusResponseType.class));
    }
}
