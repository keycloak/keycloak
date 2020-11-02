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

package org.keycloak.configuration;

import java.util.Collections;
import java.util.Map;

import io.smallrye.config.common.AbstractConfigSource;
import org.keycloak.quarkus.KeycloakRecorder;

/**
 * A {@link org.eclipse.microprofile.config.spi.ConfigSource} based on the configuration properties persisted into the server
 * image.
 */
public class PersistedConfigSource extends AbstractConfigSource {

    public PersistedConfigSource() {
        super("PersistedConfigSource", 300);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public String getValue(String propertyName) {
        String canonicalFormat = PropertyMappers.canonicalFormat(propertyName);
        String value = KeycloakRecorder.getBuiltTimeProperty(canonicalFormat);

        if (value != null) {
            return value;
        }
        
        return KeycloakRecorder.getBuiltTimeProperty(propertyName);
    }
}
