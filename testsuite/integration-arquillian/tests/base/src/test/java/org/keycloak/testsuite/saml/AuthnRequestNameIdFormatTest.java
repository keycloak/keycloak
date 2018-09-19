/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.util.List;
import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.keycloak.testsuite.util.SamlClient.*;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class AuthnRequestNameIdFormatTest extends AbstractSamlTest {

    private void testLoginWithNameIdPolicy(Binding requestBinding, Binding responseBinding, NameIDPolicyType nameIDPolicy, Matcher<String> nameIdMatcher) throws Exception {
        SAMLDocumentHolder res = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, requestBinding)
            .transformObject(so -> {
              so.setProtocolBinding(requestBinding.getBindingUri());
              so.setNameIDPolicy(nameIDPolicy);
              return so;
            })
            .build()

          .login().user(bburkeUser).build()

          .getSamlResponse(responseBinding);

        assertThat(res.getSamlObject(), notNullValue());
        assertThat(res.getSamlObject(), instanceOf(ResponseType.class));

        ResponseType rt = (ResponseType) res.getSamlObject();
        assertThat(rt.getAssertions(), not(empty()));
        assertThat(rt.getAssertions().get(0).getAssertion().getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));
        NameIDType nameId = (NameIDType) rt.getAssertions().get(0).getAssertion().getSubject().getSubType().getBaseID();
        assertThat(nameId.getValue(), nameIdMatcher);
    }

    @Test
    public void testPostLoginNameIdPolicyUnspecified() throws Exception {
        NameIDPolicyType nameIdPolicy = new NameIDPolicyType();
        nameIdPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.getUri());
        testLoginWithNameIdPolicy(Binding.POST, Binding.POST, nameIdPolicy, is("bburke"));
    }

    @Test
    public void testPostLoginNameIdPolicyEmail() throws Exception {
        NameIDPolicyType nameIdPolicy = new NameIDPolicyType();
        nameIdPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.getUri());
        testLoginWithNameIdPolicy(Binding.POST, Binding.POST, nameIdPolicy, is("bburke@redhat.com"));
    }

    @Test
    public void testPostLoginNameIdPolicyPersistent() throws Exception {
        NameIDPolicyType nameIdPolicy = new NameIDPolicyType();
        nameIdPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.getUri());
        testLoginWithNameIdPolicy(Binding.POST, Binding.POST, nameIdPolicy, startsWith("G-"));
    }

    @Test
    public void testPostLoginNoNameIdPolicyUnset() throws Exception {
        testLoginWithNameIdPolicy(Binding.POST, Binding.POST, null, is("bburke"));
    }

    @Test
    public void testRedirectLoginNameIdPolicyUnspecified() throws Exception {
        NameIDPolicyType nameIdPolicy = new NameIDPolicyType();
        nameIdPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.getUri());
        testLoginWithNameIdPolicy(Binding.REDIRECT, Binding.REDIRECT, nameIdPolicy, is("bburke"));
    }

    @Test
    public void testRedirectLoginNameIdPolicyEmail() throws Exception {
        NameIDPolicyType nameIdPolicy = new NameIDPolicyType();
        nameIdPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.getUri());
        testLoginWithNameIdPolicy(Binding.REDIRECT, Binding.REDIRECT, nameIdPolicy, is("bburke@redhat.com"));
    }

    @Test
    public void testRedirectLoginNameIdPolicyPersistent() throws Exception {
        NameIDPolicyType nameIdPolicy = new NameIDPolicyType();
        nameIdPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.getUri());
        testLoginWithNameIdPolicy(Binding.REDIRECT, Binding.REDIRECT, nameIdPolicy, startsWith("G-"));
    }

    @Test
    public void testRedirectLoginNoNameIdPolicyUnset() throws Exception {
        testLoginWithNameIdPolicy(Binding.REDIRECT, Binding.REDIRECT, null, is("bburke"));
    }

    @Test
    public void testRedirectLoginNoNameIdPolicyForcePostBinding() throws Exception {
        ClientsResource clients = adminClient.realm(REALM_NAME).clients();
        List<ClientRepresentation> foundClients = clients.findByClientId(SAML_CLIENT_ID_SALES_POST);
        assertThat(foundClients, hasSize(1));
        ClientResource clientRes = clients.get(foundClients.get(0).getId());
        ClientRepresentation client = clientRes.toRepresentation();
        client.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "true");
        clientRes.update(client);

        testLoginWithNameIdPolicy(Binding.REDIRECT, Binding.POST, null, is("bburke"));

        // Revert
        client = clientRes.toRepresentation();
        client.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false");
        clientRes.update(client);
    }

}
