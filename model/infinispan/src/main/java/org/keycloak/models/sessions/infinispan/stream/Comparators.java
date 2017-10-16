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

import org.keycloak.models.sessions.infinispan.UserSessionTimestamp;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Comparators {

    public static Comparator<UserSessionTimestamp> userSessionTimestamp() {
        return new UserSessionTimestampComparator();
    }

    private static class UserSessionTimestampComparator implements Comparator<UserSessionTimestamp>, Serializable {
        @Override
        public int compare(UserSessionTimestamp u1, UserSessionTimestamp u2) {
            return u1.getClientSessionTimestamp() - u2.getClientSessionTimestamp();
        }
    }


    public static Comparator<UserSessionEntity> userSessionLastSessionRefresh() {
        return new UserSessionLastSessionRefreshComparator();
    }

    private static class UserSessionLastSessionRefreshComparator implements Comparator<UserSessionEntity>, Serializable {

        @Override
        public int compare(UserSessionEntity u1, UserSessionEntity u2) {
            return u1.getLastSessionRefresh() - u2.getLastSessionRefresh();
        }
    }

}
