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

package org.keycloak.tests.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * Verifies that user searches only issue {@code SELECT DISTINCT} when the query joins other tables.
 * A plain listing must not use it, otherwise the database cannot page using the username index
 * (see https://github.com/keycloak/keycloak/issues/51385). Searches joining attributes or federated
 * identities still need it, otherwise users with several matching rows are returned more than once
 * (see https://github.com/keycloak/keycloak/issues/39246).
 */
@KeycloakIntegrationTest
public class UserSearchDistinctTest {

    private static final String REALM_NAME = "user-search-distinct";
    private static final String SQL_LOGGER = "org.hibernate.SQL";
    private static final String SHARED_IDP_USER_ID = "shared-idp-user-id";

    @InjectRealm(lifecycle = LifeCycle.METHOD, config = UserSearchDistinctRealm.class)
    ManagedRealm managedRealm;

    @TestOnServer
    public void plainSearchDoesNotUseDistinct(KeycloakSession session) {
        createUsers(session);

        List<String> statements = Collections.synchronizedList(new ArrayList<>());
        List<String> usernames = captureSql(statements,
                () -> search(session, Map.of(UserModel.INCLUDE_SERVICE_ACCOUNT, Boolean.FALSE.toString()), 1, 2));

        assertThat(usernames, contains("idp-user", "user-1"));
        assertThat(userSearchStatement(statements), not(startsWith("select distinct")));
    }

    @TestOnServer
    public void searchByMultiValuedAttributeUsesDistinct(KeycloakSession session) {
        createUsers(session);

        List<String> statements = Collections.synchronizedList(new ArrayList<>());
        List<String> usernames = captureSql(statements,
                () -> search(session, Map.of("attr", "common", UserModel.EXACT, Boolean.FALSE.toString()), null, null));

        assertThat(usernames, contains("attribute-user"));
        assertThat(userSearchStatement(statements), startsWith("select distinct"));
    }

    @TestOnServer
    public void searchByIdpUserIdUsesDistinct(KeycloakSession session) {
        createUsers(session);

        List<String> statements = Collections.synchronizedList(new ArrayList<>());
        List<String> usernames = captureSql(statements,
                () -> search(session, Map.of(UserModel.IDP_USER_ID, SHARED_IDP_USER_ID), null, null));

        assertThat(usernames, contains("idp-user"));
        assertThat(userSearchStatement(statements), startsWith("select distinct"));
    }

    private static void createUsers(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);

            for (int i = 1; i <= 3; i++) {
                s.users().addUser(realm, "user-" + i);
            }

            UserModel attributeUser = s.users().addUser(realm, "attribute-user");
            attributeUser.setAttribute("attr", List.of("common", "common2"));

            UserModel idpUser = s.users().addUser(realm, "idp-user");
            s.users().addFederatedIdentity(realm, idpUser, new FederatedIdentityModel("idp-a", SHARED_IDP_USER_ID, "idp-user"));
            s.users().addFederatedIdentity(realm, idpUser, new FederatedIdentityModel("idp-b", SHARED_IDP_USER_ID, "idp-user"));
        });
    }

    private static List<String> search(KeycloakSession session, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            return s.users().searchForUserStream(realm, attributes, firstResult, maxResults)
                    .map(UserModel::getUsername)
                    .collect(Collectors.toList());
        }, "user search");
    }

    private static <T> T captureSql(List<String> statements, Supplier<T> task) {
        Logger logger = Logger.getLogger(SQL_LOGGER);
        Level originalLevel = logger.getLevel();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                statements.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.FINE);
        logger.addHandler(handler);
        try {
            return task.get();
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
        }
    }

    private static String userSearchStatement(List<String> statements) {
        List<String> candidates = new ArrayList<>(statements).stream()
                .map(statement -> statement.toLowerCase().replaceAll("\\s+", " ").trim())
                .filter(statement -> statement.contains("from user_entity") && statement.contains("order by"))
                .collect(Collectors.toList());

        assertThat("Expected exactly one user search statement in " + statements, candidates, hasSize(1));
        return candidates.get(0);
    }

    private static final class UserSearchDistinctRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(REALM_NAME);
        }
    }

}
