/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.common;

import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Wrapper for adapters around handling time in seconds.
 *
 * Will be removed once <a href="https://github.com/keycloak/keycloak/issues/11053">#11053</a> has been implemented.

 * @author Alexander Schwartz
 */
public class TimeAdapter {
    private static final Logger LOG = Logger.getLogger(TimeAdapter.class);

    /**
     * Wrapper to all unsafe downgrading from a Long to an Integer while Keycloak core still handles all time since 1970 as seconds as integers.
     * This is safer to use than downgrading in several places as that might be missed once the Core starts to use longs as timestamps as well.
     * Simplify/remove once <a href="https://github.com/keycloak/keycloak/issues/11053">#11053</a> has been implemented.
     */

    public static int fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(Long timestamp) {
        if (timestamp > Integer.MAX_VALUE) {
            LOG.warn("Trimmed time value found in the map store; value too large and not supported in core");
            return Integer.MAX_VALUE;
        } else {
            return timestamp.intValue();
        }
    }

    /**
     * Wrapper to all upgrading from an Integer to a Long while Keycloak core still handles all time seconds since 1970 as seconds as integers.
     * This is safer to use and remove once the Core starts to use longs as timestamps as well.
     * Simplify/remove once <a href="https://github.com/keycloak/keycloak/issues/11053">#11053</a> has been implemented.
     */
    public static long fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(int timestamp) {
        return timestamp;
    }

    public static Long fromSecondsToMilliseconds(Long seconds) {
        if (seconds == null) return null;
        return TimeUnit.SECONDS.toMillis(seconds);
    }

    public static Long fromMilliSecondsToSeconds(Long milliSeconds) {
        if (milliSeconds == null) return null;
        return TimeUnit.MILLISECONDS.toSeconds(milliSeconds);
    }

    public static Long fromSecondsToMilliseconds(int seconds) {
        return fromSecondsToMilliseconds(fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(seconds));
    }
}
