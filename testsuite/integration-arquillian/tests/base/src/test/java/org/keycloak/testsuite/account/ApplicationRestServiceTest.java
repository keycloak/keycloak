/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.account;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.account.ApplicationResource;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationRestServiceTest extends AbstractRestServiceTest {

    private static final String THIRD_PARTY_APP = "third-party";
    private static final String REALM_NAME = "test";

    @Page
    protected OAuthGrantPage grantPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("app-1")
                .name("app-1")
                .baseUrl("http://client0.example.com")
                .redirectUris(OAuthClient.APP_ROOT + "/auth")
                .consentRequired(true)
                .fullScopeEnabled(false)
                .publicClient().build());

        testRealm.getUsers().add(UserBuilder.create().username("alice").password("password").enabled(true)
                .role("account", AccountRoles.VIEW_APPLICATIONS).build());
    }

    @Test
    public void testGetApplications() throws IOException {
        TokenUtil viewToken = new TokenUtil("alice", "password");
        oauth.doLogin("alice", "password");

        List<ApplicationResource.ApplicationRepresentation> sessions = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient).auth(viewToken.getToken())
                .asJson(new TypeReference<List<ApplicationResource.ApplicationRepresentation>>() {
                });

        //TODO: assert response
    }

    // KEYCLOAK-5155
    @Test
    public void testConsoleListedInApplications() throws IOException {
        TokenUtil viewToken = new TokenUtil("alice", "password");
        oauth.doLogin("alice", "password");

        List<ApplicationResource.ApplicationRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient).auth(viewToken.getToken())
                .asJson(new TypeReference<List<ApplicationResource.ApplicationRepresentation>>() {
                });
        Assert.assertThat(applications, hasItems(hasProperty("name", is("Admin CLI"))));
    }
}
