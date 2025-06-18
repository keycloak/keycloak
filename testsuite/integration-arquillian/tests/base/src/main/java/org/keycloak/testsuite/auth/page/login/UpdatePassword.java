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

package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.auth.page.PasswordFields;

/**
 *
 * @author tkyjovsk
 */
public class UpdatePassword extends RequiredActions {

    @Page
    private PasswordFields passwordFields;

    @Override
    public String getActionId() {
        return UserModel.RequiredAction.UPDATE_PASSWORD.name();
    }

    public void updatePasswords(String newPassword, String confirmPassword) {
        passwordFields.setNewPassword(newPassword);
        passwordFields.setConfirmPassword(confirmPassword);
        submit();
    }

    public PasswordFields fields() {
        return passwordFields;
    }

}
