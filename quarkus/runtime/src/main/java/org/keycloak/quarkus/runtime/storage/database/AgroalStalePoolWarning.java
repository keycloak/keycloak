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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.common.util.DurationConverter;

import io.agroal.api.AgroalDataSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Warns when the Agroal connection pool can run into its stale pool handling bug.
 * <p>
 * In the affected Agroal versions, a failed background validation of an idle connection makes the pool discard every connection
 * on borrow and create new ones without bound, as long as connections are validated on borrow.
 * Keycloak doesn't enable validation on borrow by default, therefore only custom Quarkus settings can trigger this.
 * <p>
 * TODO: Remove this class, its call and its test once Agroal is updated to 3.3 or later, see https://github.com/keycloak/keycloak/issues/55410
 */
public final class AgroalStalePoolWarning {

    private static final Logger logger = Logger.getLogger(AgroalStalePoolWarning.class);

    static final String VALIDATE_ON_BORROW = "quarkus.datasource.jdbc.validate-on-borrow";
    static final String FOREGROUND_VALIDATION_INTERVAL = "quarkus.datasource.jdbc.foreground-validation-interval";
    static final String BACKGROUND_VALIDATION_INTERVAL = "quarkus.datasource.jdbc.background-validation-interval";

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)");

    private AgroalStalePoolWarning() {
    }

    public static void warnIfAffected() {
        warnIfAffected(AgroalDataSource.class.getPackage().getImplementationVersion(), ConfigProvider.getConfig());
    }

    static boolean warnIfAffected(String agroalVersion, Config config) {
        if (!isAffectedVersion(agroalVersion)) {
            return false;
        }

        boolean validateOnBorrow = config.getOptionalValue(VALIDATE_ON_BORROW, Boolean.class).orElse(false);
        boolean foregroundValidation = config.getOptionalValue(FOREGROUND_VALIDATION_INTERVAL, String.class).isPresent();
        if (!validateOnBorrow && !foregroundValidation) {
            return false;
        }

        // Quarkus enables background validation by default, which is what flags the pool as stale
        boolean backgroundValidation = config.getOptionalValue(BACKGROUND_VALIDATION_INTERVAL, String.class)
                .map(value -> !DurationConverter.parseDuration(value).isZero())
                .orElse(true);
        if (!backgroundValidation) {
            return false;
        }

        logger.warnf("The settings '%s' or '%s' combined with background validation can make Agroal %s open database connections without bound "
                        + "after a single failed validation of an idle connection. "
                        + "Remove these settings. "
                        + "See https://github.com/agroal/agroal/issues/217 for details.",
                VALIDATE_ON_BORROW, FOREGROUND_VALIDATION_INTERVAL, agroalVersion);
        return true;
    }

    /**
     * The stale pool handling was added in Agroal 3.1 and fixed in 3.3.
     */
    static boolean isAffectedVersion(String version) {
        return Optional.ofNullable(version)
                .map(VERSION::matcher)
                .filter(Matcher::find)
                .map(m -> {
                    int major = Integer.parseInt(m.group(1));
                    int minor = Integer.parseInt(m.group(2));
                    return major == 3 && minor >= 1 && minor < 3;
                })
                .orElse(false);
    }
}
