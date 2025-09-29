/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oauth;

import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.protocol.oauth2.OAuth2WellKnownProviderFactory;
import org.keycloak.services.resources.ServerMetadataResource;
import org.keycloak.testsuite.oidc.AbstractWellKnownProviderTest;

import java.net.URI;

public class RFC8414CompliantOAuth2WellKnownProviderTest extends AbstractWellKnownProviderTest {

    protected String getWellKnownProviderId() {
        return OAuth2WellKnownProviderFactory.PROVIDER_ID;
    }

    protected URI getOIDCDiscoveryUri(UriBuilder builder) {
        return ServerMetadataResource.wellKnownOAuthProviderUrl(builder).build(this.getWellKnownProviderId(), "test");
    }

}
