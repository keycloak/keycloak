/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Utility to apply correlation-mitigation to time-related claims
 * by either randomizing within a window or rounding to a unit.
 * <p>
 * Configuration via realm attributes (all optional):
 * - oid4vci.time.claims.strategy: off | randomize | round (default: off)
 * - oid4vci.time.randomize.window.seconds: integer seconds (default: 86400)
 * - oid4vci.time.round.unit: SECOND | MINUTE | HOUR | DAY (default: SECOND)
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class TimeClaimNormalizer {

    private static final Logger logger = Logger.getLogger(TimeClaimNormalizer.class);

    public enum Strategy {
        OFF,
        RANDOMIZE,
        ROUND
    }

    public enum RoundUnit {
        SECOND,
        MINUTE,
        HOUR,
        DAY
    }

    private final Strategy strategy;
    private final long randomizeWindowSeconds;
    private final RoundUnit roundUnit;

    public static final long DEFAULT_RANDOMIZE_WINDOW = 86400; // 24h default
    public static final Strategy DEFAULT_STRATEGY = Strategy.OFF;
    public static final RoundUnit DEFAULT_ROUND_UNIT = RoundUnit.SECOND;

    public TimeClaimNormalizer(KeycloakSession session) {
        this(session.getContext().getRealm());
    }

    public TimeClaimNormalizer(RealmModel realm) {
        this.strategy = parseStrategy(realm.getAttribute(OID4VCIConstants.TIME_CLAIMS_STRATEGY));
        this.randomizeWindowSeconds = parseRandomizeWindow(realm.getAttribute(OID4VCIConstants.TIME_RANDOMIZE_WINDOW_SECONDS));
        this.roundUnit = parseRoundUnit(realm.getAttribute(OID4VCIConstants.TIME_ROUND_UNIT));
    }

    TimeClaimNormalizer(Strategy strategy, Long randomizeWindowSeconds, RoundUnit roundUnit) {
        this.strategy = strategy == null ? DEFAULT_STRATEGY : strategy;
        this.randomizeWindowSeconds =
                randomizeWindowSeconds == null ? DEFAULT_RANDOMIZE_WINDOW : randomizeWindowSeconds;
        this.roundUnit = roundUnit == null ? DEFAULT_ROUND_UNIT : roundUnit;
    }

    public Instant normalize(Instant original) {
        if (original == null) {
            return null;
        }
        return switch (strategy) {
            case RANDOMIZE -> randomize(original);
            case ROUND -> round(original);
            case OFF -> original;
        };
    }

    private Instant randomize(Instant original) {
        long randomOffset = (long) (Math.random() * (randomizeWindowSeconds + 1));
        return original.minusSeconds(randomOffset);
    }

    private Instant round(Instant original) {
        // Truncate in UTC by design to ensure consistent, timezone-independent rounding
        ZonedDateTime zdt = original.atZone(ZoneOffset.UTC);
        return switch (roundUnit) {
            case SECOND -> zdt.truncatedTo(ChronoUnit.SECONDS).toInstant();
            case MINUTE -> zdt.truncatedTo(ChronoUnit.MINUTES).toInstant();
            case HOUR -> zdt.truncatedTo(ChronoUnit.HOURS).toInstant();
            case DAY -> zdt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        };
    }

    private static Strategy parseStrategy(String value) {
        if (value == null) {
            return DEFAULT_STRATEGY;
        }
        try {
            return Strategy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warnf("Invalid time-claim strategy '%s'. Using default '%s'", value, DEFAULT_STRATEGY);
            return DEFAULT_STRATEGY;
        }
    }

    private static long parseRandomizeWindow(String value) {
        if (StringUtil.isBlank(value)) {
            return DEFAULT_RANDOMIZE_WINDOW;
        }
        try {
            long window = Long.parseLong(value.trim());
            if (window <= 0) {
                logger.warnf("Randomization window is zero or negative (%d), will be using default value", window);
                return DEFAULT_RANDOMIZE_WINDOW;
            }
            return window;
        } catch (NumberFormatException ex) {
            logger.warnf("Invalid randomize window '%s'. Using default %d seconds", value, DEFAULT_RANDOMIZE_WINDOW);
            return DEFAULT_RANDOMIZE_WINDOW;
        }
    }

    private static RoundUnit parseRoundUnit(String value) {
        if (value == null) {
            return DEFAULT_ROUND_UNIT;
        }
        try {
            return RoundUnit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warnf("Invalid round unit '%s'. Using default '%s'", value, DEFAULT_ROUND_UNIT);
            return DEFAULT_ROUND_UNIT;
        }
    }
}
