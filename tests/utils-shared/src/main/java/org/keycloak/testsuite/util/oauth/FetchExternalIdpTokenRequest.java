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

package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class FetchExternalIdpTokenRequest extends AbstractHttpGetRequest<AccessTokenResponse> {

    private final String providerAlias;
    private final String accessToken;

    FetchExternalIdpTokenRequest(String providerAlias, String accessToken, AbstractOAuthClient<?> client) {
        super(client);
        this.providerAlias = providerAlias;
        this.accessToken = accessToken;
    }

    @Override
    protected String getEndpoint() {
        return UriBuilder.fromUri(client.baseUrl).path("/realms/{realm-name}/broker/{provider_alias}/token").buildFromMap(Map.of("realm-name", client.config.getRealm(), "provider_alias", providerAlias)).toString();
    }

    protected void initRequest() {
        header("Authorization", "Bearer " + accessToken);

        if (client.config.getOrigin() != null) {
            header("Origin", client.config.getOrigin());
        }
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response) {
            @Override
            protected void parseError() throws IOException {
                ObjectNode json = asJson(ObjectNode.class);
                setError(json.has("errorMessage") ? json.get("errorMessage").asText() : null);
            }
        };
    }

}
