/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SAMLClientRegistrationTest extends AbstractClientRegistrationTest {

    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    @Test
    public void createClient() throws ClientRegistrationException, IOException {
        String entityDescriptor = IOUtils.toString(getClass().getResourceAsStream("/clientreg-test/saml-entity-descriptor.xml"));
        ClientRepresentation response = reg.saml().create(entityDescriptor);

        assertThat(response.getRegistrationAccessToken(), notNullValue());
        assertThat(response.getClientId(), is("loadbalancer-9.siroe.com"));
        assertThat(response.getRedirectUris(), containsInAnyOrder(
          "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/post",
          "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/soap",
          "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/paos",
          "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/redirect"
        ));  // No redirect URI for ARTIFACT binding which is unsupported

        assertThat(response.getAttributes().get("saml_single_logout_service_url_redirect"), is("https://LoadBalancer-9.siroe.com:3443/federation/SPSloRedirect/metaAlias/sp"));
    }

}
