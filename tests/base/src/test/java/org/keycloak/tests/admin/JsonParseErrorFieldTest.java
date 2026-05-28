/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest
public class JsonParseErrorFieldTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void numericFieldOutOfRangeNamesFieldWithoutEchoingValue() throws Exception {
        // notBefore is an Integer on ClientRepresentation. A value above
        // Integer.MAX_VALUE (2147483648) overflows when Jackson deserializes the
        // request body, which previously surfaced as a generic "Cannot parse the
        // JSON" with no indication of which input was at fault. The error now
        // names the offending field so the caller can locate it, while never
        // echoing the submitted value back.
        String url = keycloakUrls.getAdminBuilder()
                .path("realms/{realm}/clients")
                .build(managedRealm.getName())
                .toString();

        HttpPost post = new HttpPost(url);
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.tokenManager().getAccessTokenString());
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        post.setEntity(new StringEntity("{\"clientId\":\"out-of-range-client\",\"notBefore\":2147483648}"));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            String body = EntityUtils.toString(response.getEntity());
            assertThat(body, containsString("notBefore"));
            assertThat(body, not(containsString("2147483648")));
        }
    }
}
