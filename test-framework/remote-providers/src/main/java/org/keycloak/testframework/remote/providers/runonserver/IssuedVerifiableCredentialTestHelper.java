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

package org.keycloak.testframework.remote.providers.runonserver;

import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.UserVerifiableCredentialModel;

public final class IssuedVerifiableCredentialTestHelper {

    private IssuedVerifiableCredentialTestHelper() {
    }

    public static RunOnServer add(String userId, String scopeId, Long expiresAt) {
        return session -> {
            UserVerifiableCredentialModel credential = session.users().addVerifiableCredential(userId,
                    new UserVerifiableCredentialModel(null, scopeId));
            IssuedVerifiableCredentialModel issuedCredential = new IssuedVerifiableCredentialModel(
                    userId, credential.getId(), "wallet-client");
            issuedCredential.setExpiresAt(expiresAt);
            session.users().addIssuedVerifiableCredential(issuedCredential);
        };
    }

    public static FetchOnServer count(String userId) {
        return session -> session.users().getIssuedVerifiableCredentialsStreamByUser(userId).count();
    }

    public static FetchOnServer removeUserCredential(String userId, String scopeId) {
        return session -> session.users().removeVerifiableCredential(userId, scopeId);
    }

    public static RunOnServer removeExpired() {
        return session -> session.users().removeExpiredIssuedVerifiableCredentials();
    }
}
