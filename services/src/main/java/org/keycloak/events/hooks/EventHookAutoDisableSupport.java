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

package org.keycloak.events.hooks;

import java.util.LinkedHashMap;
import java.util.Map;

final class EventHookAutoDisableSupport {

    static final String LEGACY_AUTO_DISABLED_UNTIL = "_autoDisabledUntil";
    static final String LEGACY_AUTO_DISABLED_REASON = "_autoDisabledReason";
    static final String LEGACY_CONSECUTIVE_429_COUNT = "_consecutive429Count";
    static final int DEFAULT_AUTO_DISABLE_AFTER_429_COUNT = 3;
    static final long DEFAULT_AUTO_DISABLE_DURATION_MS = 300_000L;
    static final String AUTO_DISABLED_REASON = "Automatically disabled after repeated 429 responses";

    private EventHookAutoDisableSupport() {
    }

    static Map<String, Object> redactLegacyState(Map<String, Object> settings) {
        Map<String, Object> redacted = new LinkedHashMap<>(settings == null ? Map.of() : settings);
        redacted.remove(LEGACY_AUTO_DISABLED_UNTIL);
        redacted.remove(LEGACY_AUTO_DISABLED_REASON);
        redacted.remove(LEGACY_CONSECUTIVE_429_COUNT);
        return redacted;
    }

    static boolean isAutoDisabled(EventHookTargetModel target) {
        return target != null && !target.isEnabled() && autoDisabledUntil(target) != null;
    }

    static long deliveryPausedUntil(EventHookTargetModel target, long now) {
        Long autoDisabledUntil = autoDisabledUntil(target);
        return autoDisabledUntil != null && autoDisabledUntil.longValue() > now ? autoDisabledUntil.longValue() : now;
    }

    static boolean shouldProcessEvents(EventHookTargetModel target) {
        return target != null && (target.isEnabled() || isAutoDisabled(target));
    }

    static boolean shouldResume(EventHookTargetModel target, long now) {
        Long autoDisabledUntil = autoDisabledUntil(target);
        return isAutoDisabled(target) && autoDisabledUntil != null && autoDisabledUntil.longValue() <= now;
    }

    static void resume(EventHookTargetModel target, long now) {
        if (target == null) {
            return;
        }

        target.setSettings(clearLegacyState(target.getSettings()));
        target.setAutoDisabledUntil(null);
        target.setAutoDisabledReason(null);
        target.setConsecutive429Count(null);
        target.setEnabled(true);
        target.setUpdatedAt(now);
    }

    static long applyAutoDisableState(EventHookTargetModel target, EventHookDeliveryResult result, long now) {
        if (target == null || result == null) {
            return now;
        }

        boolean autoDisabled = isAutoDisabled(target);
        if (!result.isAutoDisableEligible()) {
            target.setSettings(clearLegacyState(target.getSettings()));
            target.setConsecutive429Count(null);
            target.setAutoDisabledUntil(null);
            target.setAutoDisabledReason(null);
            if (autoDisabled) {
                target.setEnabled(true);
            }
            target.setUpdatedAt(now);
            return now;
        }

        int consecutiveEligibleErrors = consecutiveEligibleErrors(target) + 1;
        int disableThreshold = intSetting(target.getSettings(), "autoDisableAfter429Count", DEFAULT_AUTO_DISABLE_AFTER_429_COUNT);
        long disableDuration = longSetting(target.getSettings(), "autoDisableDurationMs", DEFAULT_AUTO_DISABLE_DURATION_MS);
        long retryAfterMillis = result.getRetryAfterMillis() == null ? 0L : result.getRetryAfterMillis().longValue();
        long pausedUntil = now;

        target.setConsecutive429Count(consecutiveEligibleErrors);
        target.setSettings(clearLegacyState(target.getSettings()));

        if (consecutiveEligibleErrors >= disableThreshold) {
            long cooldownMillis = Math.max(disableDuration, retryAfterMillis);
            pausedUntil = now + cooldownMillis;
            target.setAutoDisabledUntil(pausedUntil);
            target.setAutoDisabledReason(AUTO_DISABLED_REASON);
            target.setEnabled(false);
            result.setRetryAfterMillis(Math.max(retryAfterMillis, cooldownMillis));
        } else {
            target.setAutoDisabledUntil(null);
            target.setAutoDisabledReason(null);
        }

        target.setUpdatedAt(now);
        return pausedUntil;
    }

    private static Long autoDisabledUntil(EventHookTargetModel target) {
        if (target == null) {
            return null;
        }
        if (target.getAutoDisabledUntil() != null) {
            return target.getAutoDisabledUntil();
        }

        Object value = target.getSettings() == null ? null : target.getSettings().get(LEGACY_AUTO_DISABLED_UNTIL);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static int consecutiveEligibleErrors(EventHookTargetModel target) {
        if (target != null && target.getConsecutive429Count() != null) {
            return target.getConsecutive429Count().intValue();
        }

        return intSetting(target == null ? null : target.getSettings(), LEGACY_CONSECUTIVE_429_COUNT, 0);
    }

    private static Map<String, Object> clearLegacyState(Map<String, Object> settings) {
        if (settings == null || settings.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> updated = new LinkedHashMap<>(settings);
        updated.remove(LEGACY_AUTO_DISABLED_UNTIL);
        updated.remove(LEGACY_AUTO_DISABLED_REASON);
        updated.remove(LEGACY_CONSECUTIVE_429_COUNT);
        return updated.isEmpty() ? Map.of() : updated;
    }

    private static int intSetting(Map<String, Object> settings, String key, int defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static long longSetting(Map<String, Object> settings, String key, long defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
