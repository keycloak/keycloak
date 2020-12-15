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

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CredentialInputUpdater {
    boolean supportsCredentialType(String credentialType);
    boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input);
    void disableCredentialType(RealmModel realm, UserModel user, String credentialType);

    /**
     *
     * Returns a set of credential types that can be disabled by disableCredentialType() method
     *
     * @param realm
     * @param user
     * @return
     * @deprecated Use {@link #getDisableableCredentialTypesStream(RealmModel, UserModel) getDisableableCredentialTypesStream}
     * instead.
     */
    @Deprecated
    Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user);

    /**
     * Obtains the set of credential types that can be disabled via {@link #disableCredentialType(RealmModel, UserModel, String)
     * disableCredentialType}.
     *
     * @param realm a reference to the realm.
     * @param user the user whose credentials are being searched.
     * @return a non-null {@link Stream} of credential types.
     */
    default Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        Set<String> result = this.getDisableableCredentialTypes(realm, user);
        return result != null ? result.stream() : Stream.empty();
    }

    /**
     * The {@link CredentialInputUpdater.Streams} interface makes all collection-based methods in {@link CredentialInputUpdater}
     * default by providing implementations that delegate to the {@link Stream}-based variants instead of the other way around.
     * <p/>
     * It allows for implementations to focus on the {@link Stream}-based approach for processing sets of data and benefit
     * from the potential memory and performance optimizations of that approach.
     */
    interface Streams extends CredentialInputUpdater {
        @Override
        default Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
            return this.getDisableableCredentialTypesStream(realm, user).collect(Collectors.toSet());
        }

        @Override
        Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user);
    }
}
