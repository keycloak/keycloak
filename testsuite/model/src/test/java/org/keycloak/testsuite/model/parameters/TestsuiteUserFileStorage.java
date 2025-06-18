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
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.model.KeycloakModelParameters;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class TestsuiteUserFileStorage extends KeycloakModelParameters {

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(UserPropertyFileStorageFactory.class)
      .build();

    private static final File CONFIG_DIR;

    static {
        try {
            CONFIG_DIR = new File(TestsuiteUserFileStorage.class.getClassLoader().getResource("file-storage-provider").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot get resource directory");
        }
    }


    public TestsuiteUserFileStorage() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

    @Override
    public <T> Stream<T> getParameters(Class<T> clazz) {
        if (UserStorageProviderModel.class.isAssignableFrom(clazz)) {
            UserStorageProviderModel propProviderRO = new UserStorageProviderModel();
            propProviderRO.setName("read-only-user-props");
            propProviderRO.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
            propProviderRO.setProviderType(UserStorageProvider.class.getName());
            propProviderRO.setConfig(new MultivaluedHashMap<>());
            propProviderRO.getConfig().putSingle("priority", Integer.toString(1));
            propProviderRO.getConfig().putSingle("propertyFile",
                    CONFIG_DIR.getAbsolutePath() + File.separator + "read-only-user-password.properties");

            UserStorageProviderModel propProviderRW = new UserStorageProviderModel();
            propProviderRW.setName("user-props");
            propProviderRW.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
            propProviderRW.setProviderType(UserStorageProvider.class.getName());
            propProviderRW.setConfig(new MultivaluedHashMap<>());
            propProviderRW.getConfig().putSingle("priority", Integer.toString(2));
            propProviderRW.getConfig().putSingle("propertyFile", CONFIG_DIR.getAbsolutePath() + File.separator + "user-password.properties");
            propProviderRW.getConfig().putSingle("federatedStorage", "true");

            return Stream.of((T) propProviderRO, (T) propProviderRW);
        } else {
            return super.getParameters(clazz);
        }
    }
}
