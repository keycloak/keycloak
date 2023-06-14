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
package org.keycloak.testsuite.arquillian;

import java.lang.annotation.Annotation;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.keycloak.testsuite.util.InfinispanContainer;
import org.keycloak.testsuite.util.LegacyKeycloakContainer;

public class LegacyKeycloakContainerProvider implements ResourceProvider {

    public static final ZeroDowntimeContainer CONTAINER = processSystemProperties();
    public enum ZeroDowntimeContainer {
        POSTGRES,
        COCKROACH,
        HOTROD,
        NONE;
    }

    private final String legacyKeycloakPort = System.getProperty("keycloak.legacy.port", "8091");

    @Override
    public boolean canProvide(Class<?> type) {
        return type.equals(LegacyKeycloakContainerProvider.class);
    }

    @Override
    public Object lookup(ArquillianResource ar, Annotation... antns) {
        return this;
    }

    private static ZeroDowntimeContainer processSystemProperties() {
        String mapStorageProfileconfig = System.getProperty("auth.server.quarkus.mapStorage.profile.config");
        if (mapStorageProfileconfig == null) return ZeroDowntimeContainer.NONE;

        switch (mapStorageProfileconfig) {

            case "jpa":
                String storageConnectionsVendor = System.getProperty("keycloak.storage.connections.vendor");
                if (storageConnectionsVendor == null) return ZeroDowntimeContainer.NONE;

                switch (storageConnectionsVendor) {

                    case "postgres":
                        return ZeroDowntimeContainer.POSTGRES;
                    case "cockroach":
                        return ZeroDowntimeContainer.COCKROACH;
                    default:
                        return ZeroDowntimeContainer.NONE;
                }

            case "hotrod":
                return ZeroDowntimeContainer.HOTROD;

            default:
                return ZeroDowntimeContainer.NONE;
        }
    }

    private LegacyKeycloakContainer createLegacyKeycloakContainer() {
        return new LegacyKeycloakContainer(System.getProperty("keycloak.legacy.version.zero.downtime", "latest"));
    }

    public LegacyKeycloakContainer get() {
        switch (CONTAINER) {

            case POSTGRES:
                return createLegacyKeycloakContainer().withCommand("start-dev", 
                        "--storage=jpa",
                        "--http-port=" + legacyKeycloakPort,
                        "--db-url=" + System.getProperty("keycloak.map.storage.connectionsJpa.url"),
                        "--db-username=" + PostgresContainerProvider.POSTGRES_DB_USER,
                        "--db-password=" + PostgresContainerProvider.POSTGRES_DB_PASSWORD);

            case COCKROACH:
                return createLegacyKeycloakContainer().withCommand("start-dev", 
                        "--storage=jpa",
                        "--http-port=" + legacyKeycloakPort,
                        "--db-url=" + System.getProperty("keycloak.map.storage.connectionsJpa.url"),
                        "--db-username=" + CockroachdbContainerProvider.COCKROACHDB_DB_USER,
                        "--db-password=" + CockroachdbContainerProvider.COCKROACHDB_DB_PASSWORD);

            case HOTROD:
                return createLegacyKeycloakContainer().withCommand("start-dev", 
                        "--storage=hotrod",
                        "--http-port=" + legacyKeycloakPort,
                        "--storage-hotrod-host=" + System.getProperty(HotRodContainerProvider.HOT_ROD_STORE_HOST_PROPERTY),
                        "--storage-hotrod-port=" + InfinispanContainer.PORT,
                        "--storage-hotrod-username=" + InfinispanContainer.USERNAME,
                        "--storage-hotrod-password=" + InfinispanContainer.PASSWORD);
            case NONE:
            default:
                return null;
        }
    }
}
