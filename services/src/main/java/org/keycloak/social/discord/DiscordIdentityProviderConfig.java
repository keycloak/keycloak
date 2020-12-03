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

package org.keycloak.social.discord;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:me@prok.pw">Sergey Shatunov</a>
 */
public class DiscordIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    private static final String ALLOWED_GUILDS_KEY = "allowedGuilds";
    private static final String GUILDS_DELIMITER = ",";
    private static final Pattern GUILDS_DELIMITER_PATTERN = Pattern.compile(GUILDS_DELIMITER);

    public DiscordIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public DiscordIdentityProviderConfig() {
    }

    public Set<Long> getAllowedGuildIDs() {
        String allowedGuilds = getConfig().get(ALLOWED_GUILDS_KEY);
        if (allowedGuilds == null || allowedGuilds.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return GUILDS_DELIMITER_PATTERN.splitAsStream(allowedGuilds).map(Long::parseUnsignedLong).collect(Collectors.toSet());
    }

    public void setAllowedGuildIDs(Set<Long> allowedGuildIDs) {
        getConfig().put(ALLOWED_GUILDS_KEY, allowedGuildIDs.stream().map(Long::toUnsignedString).collect(Collectors.joining(GUILDS_DELIMITER)));
    }
}
