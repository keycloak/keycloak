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

package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Mappers {

    public static Function<Map.Entry<String, SessionEntityWrapper>, Map.Entry<String, SessionEntity>> unwrap() {
        return new SessionUnwrap();
    }

    public static Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, String> sessionId() {
        return new SessionIdMapper();
    }

    public static Function<Map.Entry<String, SessionEntityWrapper>, SessionEntity> sessionEntity() {
        return new SessionEntityMapper();
    }

    public static Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, UserSessionEntity> userSessionEntity() {
        return new UserSessionEntityMapper();
    }

    public static Function<Map.Entry<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>>, AuthenticatedClientSessionEntity> clientSessionEntity() {
        return new AuthenticatedClientSessionEntityMapper();
    }

    public static Function<Map.Entry<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>>, LoginFailureKey> loginFailureId() {
        return new LoginFailureIdMapper();
    }


    private static class SessionUnwrap implements Function<Map.Entry<String, SessionEntityWrapper>, Map.Entry<String, SessionEntity>>, Serializable {

        @Override
        public Map.Entry<String, SessionEntity> apply(Map.Entry<String, SessionEntityWrapper> wrapperEntry) {
            return new Map.Entry<String, SessionEntity>() {

                @Override
                public String getKey() {
                    return wrapperEntry.getKey();
                }

                @Override
                public SessionEntity getValue() {
                    return wrapperEntry.getValue().getEntity();
                }

                @Override
                public SessionEntity setValue(SessionEntity value) {
                    throw new IllegalStateException("Unsupported operation");
                }

            };
        }

    }


    private static class SessionIdMapper implements Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, String>, Serializable {
        @Override
        public String apply(Map.Entry<String, SessionEntityWrapper<UserSessionEntity>> entry) {
            return entry.getKey();
        }
    }

    private static class SessionEntityMapper implements Function<Map.Entry<String, SessionEntityWrapper>, SessionEntity>, Serializable {
        @Override
        public SessionEntity apply(Map.Entry<String, SessionEntityWrapper> entry) {
            return entry.getValue().getEntity();
        }
    }

    private static class UserSessionEntityMapper implements Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, UserSessionEntity>, Serializable {

        @Override
        public UserSessionEntity apply(Map.Entry<String, SessionEntityWrapper<UserSessionEntity>> entry) {
            return entry.getValue().getEntity();
        }

    }

    private static class AuthenticatedClientSessionEntityMapper implements Function<Map.Entry<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>>, AuthenticatedClientSessionEntity>, Serializable {

        @Override
        public AuthenticatedClientSessionEntity apply(Map.Entry<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> entry) {
            return entry.getValue().getEntity();
        }

    }

    private static class LoginFailureIdMapper implements Function<Map.Entry<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>>, LoginFailureKey>, Serializable {
        @Override
        public LoginFailureKey apply(Map.Entry<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> entry) {
            return entry.getKey();
        }
    }

    private static class AuthClientSessionSetMapper implements Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, Set<String>>, Serializable {

        @Override
        public Set<String> apply(Map.Entry<String, SessionEntityWrapper<UserSessionEntity>> entry) {
            UserSessionEntity entity = entry.getValue().getEntity();
            return entity.getAuthenticatedClientSessions().keySet();
        }
    }

    public static <T> Stream<T> toStream(Collection<T> collection) {
        return collection.stream();
    }

    public static Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, Set<String>> authClientSessionSetMapper() {
        return new AuthClientSessionSetMapper();
    }


}
