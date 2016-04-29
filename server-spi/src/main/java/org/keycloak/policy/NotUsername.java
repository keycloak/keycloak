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

package org.keycloak.policy;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class NotUsername extends BasePasswordPolicy {
    private static final String NAME = "notUsername";
    private static final String INVALID_PASSWORD_NOT_USERNAME = "invalidPasswordNotUsernameMessage";

    @Override
    public Error validate(KeycloakSession session, String username, String password, PasswordPolicy policy) {
        return username.equals(password) ? new Error(INVALID_PASSWORD_NOT_USERNAME) : null;
    }

    @Override
    public Error validate(KeycloakSession session, UserModel user, String password, PasswordPolicy policy) {
        return validate(session, user.getUsername(), password, policy);
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return NAME;
    }
}
