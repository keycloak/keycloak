/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.deployment;

import org.keycloak.representations.userprofile.config.UPConfig;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that store default configuration for a User Profile provider
 */
public final class UserProfileBuildItem extends SimpleBuildItem {
    private final UPConfig defaultConfig;

    public UserProfileBuildItem(UPConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public UPConfig getDefaultConfig() {
        return defaultConfig;
    }
}