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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientInitialAccessResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.util.TokenSignatureUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InitialAccessTokenTest extends AbstractClientRegistrationTest {

    private ClientInitialAccessResource resource;

    @Before
    public void before() throws Exception {
        super.before();

        resource = adminClient.realm(REALM_NAME).clientInitialAccess();
    }

    @Test
    public void create() throws ClientRegistrationException, InterruptedException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation());

        reg.auth(Auth.token(response));

        ClientRepresentation rep = new ClientRepresentation();

        setTimeOffset(10);

        ClientRepresentation created = reg.create(rep);
        Assert.assertNotNull(created);

        try {
            reg.create(rep);
            Assert.fail("Expected exception");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createWithES256() throws JWSInputException, ClientRegistrationException {
        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES256);

            ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation());
            reg.auth(Auth.token(response));

            String token = response.getToken();

            JWSHeader header = new JWSInput(token).getHeader();
            assertEquals(Algorithm.HS256, header.getAlgorithm().name());

            ClientRepresentation rep = new ClientRepresentation();
            ClientRepresentation created = reg.create(rep);
            Assert.assertNotNull(created);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
        }
    }

    @Test
    public void createMultiple() throws ClientRegistrationException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation(0, 2));

        reg.auth(Auth.token(response));

        ClientRepresentation rep = new ClientRepresentation();

        ClientRepresentation created = reg.create(rep);
        Assert.assertNotNull(created);

        created = reg.create(rep);
        Assert.assertNotNull(created);

        try {
            reg.create(rep);
            Assert.fail("Expected exception");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createExpired() throws ClientRegistrationException, InterruptedException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation(1, 1));

        reg.auth(Auth.token(response));

        ClientRepresentation rep = new ClientRepresentation();

        setTimeOffset(10);

        try {
            reg.create(rep);
            Assert.fail("Expected exception");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createDeleted() throws ClientRegistrationException, InterruptedException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation());

        reg.auth(Auth.token(response));

        resource.delete(response.getId());

        ClientRepresentation rep = new ClientRepresentation();

        try {
            reg.create(rep);
            Assert.fail("Expected exception");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

}
