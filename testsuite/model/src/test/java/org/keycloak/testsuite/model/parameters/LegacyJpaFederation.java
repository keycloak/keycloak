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

import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.storage.UserStorageProviderSpi;
import org.keycloak.storage.federated.UserFederatedStorageProviderSpi;
import org.keycloak.storage.jpa.JpaUserFederatedStorageProviderFactory;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.keycloak.storage.clientscope.ClientScopeStorageProvider;
import org.keycloak.storage.clientscope.ClientScopeStorageProviderFactory;
import org.keycloak.storage.clientscope.ClientScopeStorageProviderModel;
import org.keycloak.storage.clientscope.ClientScopeStorageProviderSpi;
import org.keycloak.testsuite.federation.HardcodedClientScopeStorageProviderFactory;
import org.keycloak.testsuite.model.Config;

/**
 *
 * @author hmlnarik
 */
public class LegacyJpaFederation extends KeycloakModelParameters {

    private final AtomicInteger counter = new AtomicInteger();

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .addAll(LegacyJpa.ALLOWED_SPIS)
      .add(UserStorageProviderSpi.class)
      .add(UserFederatedStorageProviderSpi.class)
      .add(ClientScopeStorageProviderSpi.class)

      .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .addAll(LegacyJpa.ALLOWED_FACTORIES)
      .add(JpaUserFederatedStorageProviderFactory.class)
      .add(ClientScopeStorageProviderFactory.class)
      .build();

    public LegacyJpaFederation() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

    @Override
    public <T> Stream<T> getParameters(Class<T> clazz) {
        if (ClientScopeStorageProviderModel.class.isAssignableFrom(clazz)) {
            ClientScopeStorageProviderModel federatedStorage = new ClientScopeStorageProviderModel();
            federatedStorage.setName(HardcodedClientScopeStorageProviderFactory.PROVIDER_ID + ":" + counter.getAndIncrement());
            federatedStorage.setProviderId(HardcodedClientScopeStorageProviderFactory.PROVIDER_ID);
            federatedStorage.setProviderType(ClientScopeStorageProvider.class.getName());
            return Stream.of((T) federatedStorage);
        } else {
            return super.getParameters(clazz);
        }
    }

    @Override
    public void updateConfig(Config cf) {
        LegacyJpa.updateConfigForJpa(cf);
    }
}
