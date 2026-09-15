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

package org.keycloak.testsuite.oidc.flows;

import java.util.Collections;
import java.util.List;

import org.keycloak.events.Details;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.Before;
import org.junit.jupiter.api.Assertions;

/**
 * Test for response_type=none
 *
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
public class OIDCBasicResponseTypeNoneTest extends AbstractOIDCResponseTypeTest {

    @Before
    public void clientConfiguration() {
        clientManagerBuilder().standardFlow(true).implicitFlow(false);

        oauth.client("test-app", "password");
        oauth.responseType(OIDCResponseType.NONE);
    }


    @Override
    protected boolean isFragment() {
        return false;
    }

    @Override
    protected List<IDToken> testAuthzResponseAndRetrieveIDTokens(AuthorizationEndpointResponse authzResponse, EventRepresentation loginEvent) {
        Assertions.assertEquals(OIDCResponseType.NONE, loginEvent.getDetails().get(Details.RESPONSE_TYPE));

        Assertions.assertNull(authzResponse.getCode());
        Assertions.assertNull(authzResponse.getAccessToken());
        Assertions.assertNull(authzResponse.getIdToken());
        return Collections.emptyList();
    }
}
