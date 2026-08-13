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

package org.keycloak.authentication.requiredactions.util;

import java.util.Map;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.common.util.Time;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

public class EmailCooldownManager {

    private static final Logger logger = Logger.getLogger(EmailCooldownManager.class);

    public static final String EMAIL_RESEND_COOLDOWN_SECONDS = "emailResendCooldownSeconds";
    public static final int EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS = 30;
    private static final String KEY_EXPIRE = "expire";

    public static Long retrieveCooldownEntry(RequiredActionContext context, String keyPrefix) {
        SingleUseObjectProvider singleUseCache = context.getSession().singleUseObjects();
        Map<String, String> cooldownDetails = singleUseCache.get(getCacheKey(context, keyPrefix));
        if (cooldownDetails == null) {
            return null;
        }
        long remaining = (Long.parseLong(cooldownDetails.get(KEY_EXPIRE)) - Time.currentTime());
        // Avoid the awkward situation where due to rounding the value is zero
        return remaining > 0 ? remaining : null;
    }

    public static void addCooldownEntry(RequiredActionContext context, String keyPrefix) {
        SingleUseObjectProvider cache = context.getSession().singleUseObjects();
        long cooldownSeconds = getCooldownInSeconds(context);
        cache.put(getCacheKey(context, keyPrefix), cooldownSeconds, Map.of(KEY_EXPIRE, Long.toString(Time.currentTime() + cooldownSeconds)));
    }

    public static ProviderConfigProperty createCooldownConfigProperty() {
        ProviderConfigProperty cooldown = new ProviderConfigProperty();
        cooldown.setName(EMAIL_RESEND_COOLDOWN_SECONDS);
        cooldown.setLabel("Cooldown Between Email Resend (seconds)");
        cooldown.setHelpText("Minimum delay in seconds before another email verification email can be sent.");
        cooldown.setType(ProviderConfigProperty.STRING_TYPE);
        cooldown.setDefaultValue(String.valueOf(EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS));
        return cooldown;
    }

    private static String getCacheKey(RequiredActionContext context, String keyPrefix) {
        return keyPrefix + context.getUser().getId();
    }

    private static long getCooldownInSeconds(RequiredActionContext context) {
        try {
            RequiredActionProviderModel model = context.getRealm().getRequiredActionProviderByAlias(context.getAction());
            if (model == null || model.getConfig() == null) {
                logger.warn("No RequiredActionProviderModel found for alias: " + context.getAction());
                return EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS;
            }

            String value = model.getConfig().getOrDefault(EMAIL_RESEND_COOLDOWN_SECONDS, String.valueOf(EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS));
            return Long.parseLong(value);
        } catch (RuntimeException e) {
            logger.error("Failed to fetch cooldown from config: ", e);
            return EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS;
        }
    }
}
