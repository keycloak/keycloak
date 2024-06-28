/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

/**
 * @author <a href="mailto:external.Martin.Idel@bosch.io">Martin Idel</a>
 * @version $Revision: 1 $
 */
public abstract class UserModelDefaultMethods implements UserModel {

    @Override
    public String getFirstName() {
        return getFirstAttribute(FIRST_NAME);
    }

    @Override
    public void setFirstName(String firstName) {
        setSingleAttribute(FIRST_NAME, firstName);
    }

    @Override
    public String getLastName() {
        return getFirstAttribute(LAST_NAME);
    }

    @Override
    public void setLastName(String lastName) {
        setSingleAttribute(LAST_NAME, lastName);
    }

    @Override
    public String getEmail() {
        return getFirstAttribute(EMAIL);
    }

    @Override
    public void setEmail(String email) {
        email = email == null || email.trim().isEmpty() ? null : email.toLowerCase();
        setSingleAttribute(EMAIL, email);
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + getId();
    }
}
