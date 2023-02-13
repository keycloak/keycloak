/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import java.io.Closeable;
import javax.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;

import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.w3c.dom.Document;

/**
 *
 * @author rmartinc
 */
public class KcSamlBrokerAllowedClockSkewTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void loginClientExpiredResponseFromIdP() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);

        Document doc = SAML2Request.convert(loginRep);

        new SamlClientBuilder()
          .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()   // Request to consumer IdP
          .login().idp(bc.getIDPAlias()).build()

          .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
            .targetAttributeSamlRequest()
            .build()

          .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

          .addStep(() -> KcSamlBrokerAllowedClockSkewTest.this.setTimeOffset(-30)) // offset to the past to invalidate the request
          .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP should fail
            .build()
            .execute(hr -> assertThat(hr, statusCodeIsHC(Response.Status.BAD_REQUEST)));
    }

    @Test
    public void loginClientExpiredResponseFromIdPWithClockSkew() throws Exception {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.ALLOWED_CLOCK_SKEW, "60")
                .update()) {

            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);

            Document doc = SAML2Request.convert(loginRep);

            SAMLDocumentHolder samlResponse = new SamlClientBuilder()
              .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()   // Request to consumer IdP
              .login().idp(bc.getIDPAlias()).build()

              .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

              .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

              .addStep(() -> KcSamlBrokerAllowedClockSkewTest.this.setTimeOffset(-30)) // offset to the past but inside the clock skew
              .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP expired but valid with the clock skew
                .build()

              // first-broker flow
              .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
              .followOneRedirect()

              .getSamlResponse(SamlClient.Binding.POST);       // Response from consumer IdP

            Assert.assertThat(samlResponse, Matchers.notNullValue());
            Assert.assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        }
    }
}