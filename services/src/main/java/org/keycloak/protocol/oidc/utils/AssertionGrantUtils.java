/*
 *  Copyright 2021 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.CrossDomainTrust;
import org.keycloak.models.RealmModel;
import org.keycloak.services.util.CrossDomainTrustUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ben Cresitello-Dittmar
 * Utility class to assist processing assertion grant requests to the token endpoint
 */
public class AssertionGrantUtils {
    private static final Logger logger = Logger.getLogger(AssertionGrantUtils.class);

    /**
     * Returns true if the assertion grant flow is enabled for the client.
     *
     * @param client The client to check
     * @return True if the assertion grant flow is enabled
     */
    public static boolean isOIDCAssertionGrantEnabled(ClientModel client) {
        String enabled = client.getAttribute(Constants.OAUTH_ASSERTION_GRANT_ENABLED);
        return Boolean.parseBoolean(enabled);
    }

    /**
     * Parses the trusted issuer config from the client and loads the specified cross-domain trust entities from the realm.
     * @param realm the keycloak realm containing the cross-domain trust settings
     * @param client the client to get the configuration for
     * @return The list of cross-domain trust entities
     */
    public static List<CrossDomainTrust> getTrustedIssuerConfigs(RealmModel realm, ClientModel client){
        String configsJson = client.getAttribute(Constants.OAUTH_ASSERTION_GRANT_TRUSTED_ISSUER);
        if (configsJson == null){
            configsJson = "[]";
        }

        try {
            List<String> trustedDomainIds = JsonSerialization.readValue(configsJson, new TypeReference<List<String>>() {});

            return trustedDomainIds.stream().map(id -> CrossDomainTrustUtil.getCrossDomainTrust(realm, id)).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            logger.warnf("Failed to parse assertion grant configuration for client '%s'", client.getClientId());
            return null;
        }
    }
}