/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.BootstrapAdminOptions;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class BootstrapAdminPropertyMappers implements PropertyMapperGrouping {

    private static final String PASSWORD_SET = "bootstrap admin password is set";
    private static final String CLIENT_SECRET_SET = "bootstrap admin client secret is set";

    // We prefer validators here to isEnabled so that the options show up in help
    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(BootstrapAdminOptions.USERNAME)
                        .paramLabel("username")
                        .addValidateEnabled(BootstrapAdminPropertyMappers::isPasswordSet, PASSWORD_SET)
                        .build(),
                fromOption(BootstrapAdminOptions.PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                /*fromOption(BootstrapAdminOptions.EXPIRATION)
                        .paramLabel("expiration")
                        .isEnabled(BootstrapAdminPropertyMappers::isPasswordSet, PASSWORD_SET)
                        .build(),*/
                fromOption(BootstrapAdminOptions.CLIENT_ID)
                        .paramLabel("client id")
                        .addValidateEnabled(BootstrapAdminPropertyMappers::isClientSecretSet, CLIENT_SECRET_SET)
                        .build(),
                fromOption(BootstrapAdminOptions.CLIENT_SECRET)
                        .paramLabel("client secret")
                        .isMasked(true)
                        .build()
        );
    }

    private static boolean isPasswordSet() {
        return getOptionalKcValue(BootstrapAdminOptions.PASSWORD.getKey()).isPresent();
    }

    private static boolean isClientSecretSet() {
        return getOptionalKcValue(BootstrapAdminOptions.CLIENT_SECRET.getKey()).isPresent();
    }

}
