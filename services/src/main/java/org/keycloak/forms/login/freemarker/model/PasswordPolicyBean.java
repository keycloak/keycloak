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
package org.keycloak.forms.login.freemarker.model;

import java.util.Set;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:scherer.adi@gmail.com">Adrian Scherer</a>
 */
public class PasswordPolicyBean {

    private final RealmModel realm;

    public PasswordPolicyBean(final RealmModel realmModel) {

        this.realm = realmModel;
    }

    public Set<String> getPolicies() {

        return this.realm.getPasswordPolicy().getPolicies();
    }

    public <T> T getPolicyConfig(final String key) {

        return (T) this.realm.getPasswordPolicy().getPolicyConfig(key);
    }
}