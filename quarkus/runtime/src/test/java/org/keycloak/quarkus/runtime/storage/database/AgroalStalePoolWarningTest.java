/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.database;

import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgroalStalePoolWarningTest {

    private static SmallRyeConfig config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .build();
    }

    @Test
    public void affectedVersions() {
        assertFalse(AgroalStalePoolWarning.isAffectedVersion(null));
        assertFalse(AgroalStalePoolWarning.isAffectedVersion("3.0.1"));
        assertTrue(AgroalStalePoolWarning.isAffectedVersion("3.1"));
        assertTrue(AgroalStalePoolWarning.isAffectedVersion("3.2.1"));
        assertFalse(AgroalStalePoolWarning.isAffectedVersion("3.3"));
    }

    @Test
    public void warnsOnlyWhenBorrowValidationIsConfigured() {
        assertFalse(AgroalStalePoolWarning.warnIfAffected("3.2.1", config(Map.of())));
        assertTrue(AgroalStalePoolWarning.warnIfAffected("3.2.1",
                config(Map.of(AgroalStalePoolWarning.VALIDATE_ON_BORROW, "true"))));
        assertTrue(AgroalStalePoolWarning.warnIfAffected("3.2.1",
                config(Map.of(AgroalStalePoolWarning.FOREGROUND_VALIDATION_INTERVAL, "1S"))));
        assertFalse(AgroalStalePoolWarning.warnIfAffected("3.3",
                config(Map.of(AgroalStalePoolWarning.VALIDATE_ON_BORROW, "true"))));
        assertFalse(AgroalStalePoolWarning.warnIfAffected("3.2.1",
                config(Map.of(AgroalStalePoolWarning.VALIDATE_ON_BORROW, "true",
                        AgroalStalePoolWarning.BACKGROUND_VALIDATION_INTERVAL, "0"))));
    }
}
