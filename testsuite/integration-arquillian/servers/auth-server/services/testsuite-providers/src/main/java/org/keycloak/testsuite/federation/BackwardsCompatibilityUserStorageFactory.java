/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.federation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BackwardsCompatibilityUserStorageFactory implements UserStorageProviderFactory<BackwardsCompatibilityUserStorage> {

    public static final String PROVIDER_ID = "backwards-compatibility-storage";

    private final Map<String, BackwardsCompatibilityUserStorage.MyUser> userPasswords = new ConcurrentHashMap<>();

    @Override
    public BackwardsCompatibilityUserStorage create(KeycloakSession session, ComponentModel model) {
        return new BackwardsCompatibilityUserStorage(session, model, userPasswords);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public boolean hasUserOTP(String username) {
        BackwardsCompatibilityUserStorage.MyUser user = userPasswords.get(username);
        if (user == null) return false;
        return user.getOtp() != null;
    }

    public boolean hasRecoveryCodes(String username) {
        BackwardsCompatibilityUserStorage.MyUser user = userPasswords.get(username);
        if (user == null) return false;
        return user.getRecoveryCodes() != null;
    }
}
