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

import com.google.common.collect.Collections2;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.dom.saml.v2.assertion.ConditionAbstractType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.OneTimeUseType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.SamlClient;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.SamlClient.login;

/**
 * KEYCLOAK-4360
 * @author mrpardijs
 */
public class IncludeOneTimeUseConditionTest extends AbstractSamlTest
{
	@Test
	public void testOneTimeUseConditionIsAdded() throws Exception
	{
        testOneTimeUseConditionIncluded(Boolean.TRUE);
    }

    @Test
    public void testOneTimeUseConditionIsNotAdded() throws Exception
    {
        testOneTimeUseConditionIncluded(Boolean.FALSE);
    }

    private void testOneTimeUseConditionIncluded(Boolean oneTimeUseConditionShouldBeIncluded) throws ProcessingException, ConfigurationException, ParsingException
    {
        ClientsResource clients = adminClient.realm(REALM_NAME).clients();
        List<ClientRepresentation> foundClients = clients.findByClientId(SAML_CLIENT_ID_SALES_POST);
        assertThat(foundClients, hasSize(1));
        ClientResource clientRes = clients.get(foundClients.get(0).getId());
        ClientRepresentation client = clientRes.toRepresentation();
        client.getAttributes().put(SamlConfigAttributes.SAML_ONETIMEUSE_CONDITION, oneTimeUseConditionShouldBeIncluded.toString());
        clientRes.update(client);

        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, REALM_NAME);
        loginRep.setProtocolBinding(SamlClient.Binding.POST.getBindingUri());

        Document samlRequest = SAML2Request.convert(loginRep);
        SAMLDocumentHolder res = login(bburkeUser, getAuthServerSamlEndpoint(REALM_NAME), samlRequest, null, SamlClient.Binding.POST,
			SamlClient.Binding.POST);

        assertThat(res.getSamlObject(), notNullValue());
        assertThat(res.getSamlObject(), instanceOf(ResponseType.class));

        ResponseType rt = (ResponseType) res.getSamlObject();
        assertThat(rt.getAssertions(), not(empty()));
        final ConditionsType conditionsType = rt.getAssertions().get(0).getAssertion().getConditions();
        assertThat(conditionsType, notNullValue());
        assertThat(conditionsType.getConditions(), not(empty()));

        final List<ConditionAbstractType> conditions = conditionsType.getConditions();

        final Collection<ConditionAbstractType> oneTimeUseConditions = Collections2.filter(conditions, input -> input instanceof OneTimeUseType);

        final boolean oneTimeUseConditionAdded = !oneTimeUseConditions.isEmpty();
        assertThat(oneTimeUseConditionAdded, is(oneTimeUseConditionShouldBeIncluded));
    }


}
