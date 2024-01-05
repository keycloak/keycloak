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

package org.keycloak.models.utils;

import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Wraps a supplier method to be called once write access is required for a user
 * As long as the access is only read, nothing happens.
 */
public class ReadonlyUntilWriteUserModelDelegate extends UserModelDelegate {

    private final Supplier<UserModel> supplier;
    private boolean newDelegate = false;

    public ReadonlyUntilWriteUserModelDelegate(UserModel local, Supplier<UserModel> supplier) {
        super(local);
        this.supplier = supplier;
    }

    private void redirectToNewDelegate() {
        if (!newDelegate) {
            this.delegate = supplier.get();
            newDelegate = true;
        }
    }

    @Override
    public void setUsername(String username) {
        if (!Objects.equals(getUsername(), username)) {
            redirectToNewDelegate();
            super.setUsername(username);
        }
    }

    @Override
    public void setLastName(String lastName) {
        if (!Objects.equals(getLastName(), lastName)) {
            redirectToNewDelegate();
            super.setLastName(lastName);
        }
    }

    @Override
    public void setFirstName(String first) {
        if (!Objects.equals(getFirstName(), first)) {
            redirectToNewDelegate();
            super.setFirstName(first);
        }
    }

    @Override
    public void setEmail(String email) {
        if (!Objects.equals(getEmail(), email)) {
            redirectToNewDelegate();
            super.setEmail(email);
        }
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (!Objects.equals(getAttributeStream(name).collect(Collectors.toList()), Collections.singletonList(value))) {
            redirectToNewDelegate();
            super.setSingleAttribute(name, value);
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (!Objects.equals(getAttributeStream(name).collect(Collectors.toList()), values)) {
            redirectToNewDelegate();
            super.setAttribute(name, values);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (getAttributeStream(name).count() > 0) {
            redirectToNewDelegate();
            super.removeAttribute(name);
        }
    }
}
