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
package org.keycloak.testsuite.auth.page.account;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.auth.page.PasswordFields;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author Petr Mensik
 */
public class ChangePassword extends AccountManagement {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("password");
    }

    @Page
    private PasswordFields passwordFields;

    public void changePasswords(String password, String newPassword, String confirmPassword) {
        passwordFields.setPasswords(password, newPassword, confirmPassword);
        save();
    }

}
