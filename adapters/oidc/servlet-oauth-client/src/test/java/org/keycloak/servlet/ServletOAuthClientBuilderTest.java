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

package org.keycloak.servlet;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.enums.RelativeUrlsUsed;
import org.keycloak.representations.idm.CredentialRepresentation;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServletOAuthClientBuilderTest {

    @Test
    public void testBuilder() {
        ServletOAuthClient oauthClient = ServletOAuthClientBuilder.build(getClass().getResourceAsStream("/keycloak.json"));
        Assert.assertEquals("https://localhost:8443/auth/realms/demo/protocol/openid-connect/auth", oauthClient.getDeployment().getAuthUrl().clone().build().toString());
        Assert.assertEquals("https://localhost:8443/auth/realms/demo/protocol/openid-connect/token", oauthClient.getDeployment().getTokenUrl());
        assertEquals(RelativeUrlsUsed.NEVER, oauthClient.getRelativeUrlsUsed());
        Assert.assertEquals("customer-portal", oauthClient.getClientId());
        Assert.assertEquals("234234-234234-234234", oauthClient.getCredentials().get(CredentialRepresentation.SECRET));
        Assert.assertEquals(true, oauthClient.isPublicClient());
    }
}
