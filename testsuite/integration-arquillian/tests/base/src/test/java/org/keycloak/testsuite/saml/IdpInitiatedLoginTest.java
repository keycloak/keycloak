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

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class IdpInitiatedLoginTest extends AbstractSamlTest {

    @Test
    public void testIdpInitiatedLogin() {
        new SamlClientBuilder()
          .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
          .login().user(bburkeUser).build()
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST));
              return null;
            })
            .build()
          .execute()
        ;
    }

    @Test
    public void testTwoConsequentIdpInitiatedLogins() {
        new SamlClientBuilder()
          .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
          .login().user(bburkeUser).build()
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST));
              return null;
            })
            .build()

          .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post2").build()
          .login().sso(true).build()
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(SAML_ASSERTION_CONSUMER_URL_SALES_POST2));
              return null;
            })
            .build()

          .execute()
        ;

        final UsersResource users = adminClient.realm(REALM_NAME).users();
        final ClientsResource clients = adminClient.realm(REALM_NAME).clients();

        UserRepresentation bburkeUserRepresentation = users
          .search(bburkeUser.getUsername()).stream()
          .findFirst().get();

        List<UserSessionRepresentation> userSessions = users.get(bburkeUserRepresentation.getId()).getUserSessions();
        assertThat(userSessions, hasSize(1));
        Map<String, String> clientSessions = userSessions.get(0).getClients();

        Set<String> clientIds = clientSessions.values().stream()
          .flatMap(c -> clients.findByClientId(c).stream())
          .map(ClientRepresentation::getClientId)
          .collect(Collectors.toSet());

        assertThat(clientIds, containsInAnyOrder(SAML_CLIENT_ID_SALES_POST, SAML_CLIENT_ID_SALES_POST2));

    }

}
