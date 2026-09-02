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
package org.keycloak.credential;

import java.util.stream.Stream;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CredentialInputUpdater {
    boolean supportsCredentialType(String credentialType);
    boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input);
    void disableCredentialType(RealmModel realm, UserModel user, String credentialType);

    /**
     * Obtains the set of credential types that can be disabled via {@link #disableCredentialType(RealmModel, UserModel, String)
     * disableCredentialType}.
     *
     * @param realm a reference to the realm.
     * @param user the user whose credentials are being searched.
     * @return a non-null {@link Stream} of credential types.
     */
    Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user);

    /**
     * Returns a stream of {@link CredentialModel} instances managed by this provider for the given {@code user}.
     *
     * @param realm the realm
     * @param user the user
     * @return the credentials managed by this provider for the given {@code user}
     */
    default Stream<CredentialModel> getCredentials(RealmModel realm, UserModel user) {
        return Stream.empty();
    }
}
