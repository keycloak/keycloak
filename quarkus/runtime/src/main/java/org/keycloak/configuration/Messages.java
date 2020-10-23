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

import org.keycloak.util.Environment;

public final class Messages {

    static IllegalArgumentException invalidDatabaseVendor(String db, String... availableOptions) {
        return new IllegalArgumentException("Invalid database vendor [" + db + "]. Possible values are: " + String.join(", ", availableOptions) + ".");
    }

    static IllegalArgumentException invalidProxyMode(String mode) {
        return new IllegalArgumentException("Invalid value [" + mode + "] for configuration property [proxy].");
    }

    static IllegalStateException httpsConfigurationNotSet() {
        StringBuilder builder = new StringBuilder("Key material not provided to setup HTTPS. Please configure your keys/certificates or enable HTTP");
        if (!"dev".equals(Environment.getProfile())) {
            builder.append(" or start the server using the 'dev' profile");
        }
        builder.append(".");
        return new IllegalStateException(builder.toString());
    }
}
