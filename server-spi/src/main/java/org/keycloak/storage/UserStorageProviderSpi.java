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

package org.keycloak.storage;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserStorageProviderSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "storage";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return UserStorageProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return UserStorageProviderFactory.class;
    }

    private static final List<ProviderConfigProperty> commonConfig;

    static {
        List<ProviderConfigProperty> config = ProviderConfigurationBuilder.create()
                .property()
                .name("enabled").type(ProviderConfigProperty.BOOLEAN_TYPE).add()
                .property()
                .name("priority").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("fullSyncPeriod").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("changedSyncPeriod").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("lastSync").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("batchSizeForSync").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("importEnabled").type(ProviderConfigProperty.BOOLEAN_TYPE).add()
                .property()
                .name("cachePolicy").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("maxLifespan").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("evictionHour").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("evictionMinute").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("evictionDay").type(ProviderConfigProperty.STRING_TYPE).add()
                .property()
                .name("cacheInvalidBefore").type(ProviderConfigProperty.STRING_TYPE).add()
                .build();
        commonConfig = Collections.unmodifiableList(config);
    }

    public static List<ProviderConfigProperty> commonConfig() {
        return commonConfig;

    }

}
