/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface AuthorizationProviderFactory extends ProviderFactory<AuthorizationProvider> {

    ProviderConfigProperty JPA_IN_PARAMETERS_LIMIT_THRESHOLD = ProviderConfigurationBuilder.create()
            .property()
            .name("jpaInParametersLimitThreshold")
            .label("JPA IN parameters limit threshold")
            .helpText("The maximum number of JPA IN clause bind parameters before switching to literal values. "
                    + "Set this to stay below the prepared-statement parameter limit of the database (e.g., 65,535 for PostgreSQL).")
            .type(ProviderConfigProperty.INTEGER_TYPE)
            .defaultValue(32 * 1000)
            .add()
            .build()
            .get(0);

    <C> C getConfig(ProviderConfigProperty config);

    AuthorizationProvider create(KeycloakSession session, RealmModel realm);
}
