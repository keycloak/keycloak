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
import io.fabric8.kubernetes.client.KubernetesClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ValueOrSecret {
    private String value;
    private SecretKeySelector secret;

    public ValueOrSecret() {
    }

    public ValueOrSecret(String value) {
        this.value = value;
    }

    public ValueOrSecret(SecretKeySelector secret) {
        this.secret = secret;
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

    public String readConfigurationValue(KubernetesClient client, String namespace) {
        if (this.value != null) {
            return this.value;
        } else {
            var secretSelector = this.secret;
            if (secretSelector == null) {
                throw new IllegalStateException("Secret " + this.secret.getName() + " not defined");
            }
            var secret = client.secrets().inNamespace(namespace).withName(secretSelector.getName()).get();
            if (secret == null) {
                throw new IllegalStateException("Secret " + secretSelector.getName() + " not found in cluster");
            }
            if (secret.getData().containsKey(secretSelector.getKey())) {
                return new String(Base64.getDecoder().decode(secret.getData().get(secretSelector.getKey())), StandardCharsets.UTF_8);
            } else {
                throw new IllegalStateException("Secret " + secretSelector.getName() + " doesn't contain the expected key " + secretSelector.getKey());
            }
        }
    }

}
