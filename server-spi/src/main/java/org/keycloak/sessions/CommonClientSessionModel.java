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

package org.keycloak.sessions;

import java.util.Map;
import java.util.Objects;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.util.EnumWithStableIndex;

/**
 * Predecesor of AuthenticationSessionModel, ClientLoginSessionModel and ClientSessionModel (then action tickets). Maybe we will remove it later...
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CommonClientSessionModel {

    String getRedirectUri();
    void setRedirectUri(String uri);

    RealmModel getRealm();
    ClientModel getClient();

    String getAction();
    void setAction(String action);

    String getProtocol();
    void setProtocol(String method);

    enum Action {
        OAUTH_GRANT,
        AUTHENTICATE,
        LOGGED_OUT,
        LOGGING_OUT,
        REQUIRED_ACTIONS,
        USER_CODE_VERIFICATION
    }

    enum ExecutionStatus implements EnumWithStableIndex {
        FAILED(0),
        SUCCESS(1),
        SETUP_REQUIRED(2),
        ATTEMPTED(3),
        SKIPPED(4),
        CHALLENGED(5),
        EVALUATED_TRUE(6),
        EVALUATED_FALSE(7);

        private final int stableIndex;
        private static final Map<Integer, ExecutionStatus> BY_ID = EnumWithStableIndex.getReverseIndex(values());

        private ExecutionStatus(int stableIndex) {
            Objects.requireNonNull(stableIndex);
            this.stableIndex = stableIndex;
        }

        @Override
        public int getStableIndex() {
            return stableIndex;
        }

        public static ExecutionStatus valueOfInteger(Integer id) {
            return id == null ? null : BY_ID.get(id);
        }
    }
}
