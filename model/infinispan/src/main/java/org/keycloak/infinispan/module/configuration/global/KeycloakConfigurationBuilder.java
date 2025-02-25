/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.module.configuration.global;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.keycloak.infinispan.module.certificates.JGroupsCertificateHolder;
import org.keycloak.models.KeycloakSessionFactory;

public class KeycloakConfigurationBuilder implements Builder<KeycloakConfiguration> {

    private final AttributeSet attributes;

    public KeycloakConfigurationBuilder(GlobalConfigurationBuilder unused) {
        attributes = KeycloakConfiguration.attributeSet();
    }

    @Override
    public KeycloakConfiguration create() {
        return new KeycloakConfiguration(attributes.protect());
    }

    @Override
    public Builder<?> read(KeycloakConfiguration template, Combine combine) {
        attributes.read(template.attributes(), combine);
        return this;
    }

    @Override
    public AttributeSet attributes() {
        return attributes;
    }

    @Override
    public void validate() {

    }

    public KeycloakConfigurationBuilder setKeycloakSessionFactory(KeycloakSessionFactory keycloakSessionFactory) {
        attributes.attribute(KeycloakConfiguration.KEYCLOAK_SESSION_FACTORY).set(keycloakSessionFactory);
        return this;
    }

    public KeycloakConfigurationBuilder setJGroupCertificateHolder(JGroupsCertificateHolder jGroupsCertificateHolder) {
        attributes.attribute(KeycloakConfiguration.JGROUPS_CERTIFICATE_HOLDER).set(jGroupsCertificateHolder);
        return this;
    }

    public KeycloakConfigurationBuilder setJGroupsCertificateRotation(int days) {
        attributes.attribute(KeycloakConfiguration.JGROUPS_CERTIFICATE_ROTATION).set(days);
        return this;
    }
}
