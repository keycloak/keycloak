/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1.deployment;

import io.fabric8.kubernetes.api.model.SecretKeySelector;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValueOrSecret {
    private String name;
    private String value;
    private SecretKeySelector secret;

    public ValueOrSecret() {
    }

    public ValueOrSecret(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public ValueOrSecret(String name, SecretKeySelector secret) {
        this.name = name;
        this.secret = secret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SecretKeySelector getSecret() {
        return secret;
    }

    public void setSecret(SecretKeySelector secret) {
        this.secret = secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueOrSecret that = (ValueOrSecret) o;
        return getName().equals(that.getName()); // comparing just name as it doesn't make sense to have more than one config value with the same name
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
