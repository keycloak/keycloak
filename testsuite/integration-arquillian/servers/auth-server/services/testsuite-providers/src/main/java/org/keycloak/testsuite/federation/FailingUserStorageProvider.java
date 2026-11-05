/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.federation;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserCountMethodsProvider;
import org.keycloak.storage.user.UserQueryMethodsProvider;

/**
 * Test user storage provider that simulates various failure scenarios
 * to test graceful degradation in UserStorageManager.
 */
public class FailingUserStorageProvider implements UserStorageProvider, UserQueryMethodsProvider, UserCountMethodsProvider {
    
    public static final String FAIL_ON_SEARCH = "failOnSearch";
    public static final String FAIL_ON_SEARCH_AFTER_FIRST = "failOnSearchAfterFirst";
    public static final String FAIL_ON_COUNT = "failOnCount";
    
    private final ComponentModel model;
    private final KeycloakSession session;
    
    public FailingUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        if (Boolean.parseBoolean(model.getConfig().getFirst(FAIL_ON_SEARCH))) {
            throw new RuntimeException("Simulated user search failure - LDAP connection timeout");
        }
        if (Boolean.parseBoolean(model.getConfig().getFirst(FAIL_ON_SEARCH_AFTER_FIRST))) {
            // Simulates an external user store that returns the first page(s) of results and then
            // fails while the returned stream is consumed, e.g. an LDAP server that stops
            // responding between the pages of a paginated search
            return Stream.concat(
                    Stream.of(new TestUser("lazy-fail-user1"), new TestUser("lazy-fail-user2")),
                    Stream.<UserModel>generate(() -> {
                        throw new RuntimeException("Simulated failure while streaming user search results - LDAP connection timeout between pages");
                    }));
        }
        // Return empty stream if not configured to fail
        return Stream.empty();
    }
    
    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        if (Boolean.parseBoolean(model.getConfig().getFirst(FAIL_ON_SEARCH))) {
            throw new RuntimeException("Simulated user attribute search failure - LDAP connection timeout");
        }
        return Stream.empty();
    }
    
    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        if (Boolean.parseBoolean(model.getConfig().getFirst(FAIL_ON_SEARCH))) {
            throw new RuntimeException("Simulated group member search failure - LDAP connection timeout");
        }
        return Stream.empty();
    }
    
    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        if (Boolean.parseBoolean(model.getConfig().getFirst(FAIL_ON_COUNT))) {
            throw new RuntimeException("Simulated user count failure - LDAP connection timeout");
        }
        return 0;
    }

    /**
     * Minimal in-memory user, so that the users of the first page(s) are indistinguishable from
     * users of any other provider while the search result is consumed. Everything except the
     * username is not supported.
     */
    static class TestUser extends UserModelDelegate {

        private final String username;

        TestUser(String username) {
            super(null);
            this.username = username;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }
}
