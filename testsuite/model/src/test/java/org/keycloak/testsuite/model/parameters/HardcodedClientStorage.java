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
package org.keycloak.testsuite.model.parameters;

import com.google.common.collect.ImmutableSet;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProviderSpi;
import org.keycloak.testsuite.federation.HardcodedClientStorageProviderFactory;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.KeycloakModelParameters;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class HardcodedClientStorage extends KeycloakModelParameters {
    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
            .add(ClientStorageProviderSpi.class)
            .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
            .add(HardcodedClientStorageProviderFactory.class)
            .build();

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public void updateConfig(Config cf) {
        cf.spi("client-storage").defaultProvider(HardcodedClientStorageProviderFactory.PROVIDER_ID);
    }

    @Override
    public <T> Stream<T> getParameters(Class<T> clazz) {
        if (ClientStorageProviderModel.class.isAssignableFrom(clazz)) {
            ClientStorageProviderModel clientStorage = new ClientStorageProviderModel();
            clientStorage.setName(HardcodedClientStorageProviderFactory.PROVIDER_ID + ":" + counter.getAndIncrement());
            clientStorage.setProviderId(HardcodedClientStorageProviderFactory.PROVIDER_ID);
            return Stream.of((T) clientStorage);
        } else {
            return super.getParameters(clazz);
        }
    }


    public HardcodedClientStorage() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }
}
