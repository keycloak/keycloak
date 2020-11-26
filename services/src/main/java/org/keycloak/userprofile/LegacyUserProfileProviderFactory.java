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

package org.keycloak.userprofile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class LegacyUserProfileProviderFactory implements UserProfileProviderFactory {

    private static final Logger logger = Logger.getLogger(LegacyUserProfileProviderFactory.class);

    UserProfileProvider provider;

    // Attributes, which can't be updated by user himself
    private Pattern readOnlyAttributesPattern;

    // Attributes, which can't be updated by administrator
    private Pattern adminReadOnlyAttributesPattern;

    private String[] DEFAULT_READ_ONLY_ATTRIBUTES = { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp", "userCertificate", "saml.persistent.name.id.for.*", "ENABLED", "EMAIL_VERIFIED" };
    private String[] DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES = { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp" };

    @Override
    public UserProfileProvider create(KeycloakSession session) {
        provider = new LegacyUserProfileProvider(session, readOnlyAttributesPattern, adminReadOnlyAttributesPattern);

        return provider;
    }

    @Override
    public void init(Config.Scope config) {
        this.readOnlyAttributesPattern = getRegexPatternString(config, "read-only-attributes", DEFAULT_READ_ONLY_ATTRIBUTES);
        this.adminReadOnlyAttributesPattern = getRegexPatternString(config, "admin-read-only-attributes", DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES);
    }

    private Pattern getRegexPatternString(Config.Scope config, String configKey, String[] builtinReadOnlyAttributes) {
        String[] readOnlyAttributesCfg = config.getArray(configKey);
        List<String> readOnlyAttributes = new ArrayList<>(Arrays.asList(builtinReadOnlyAttributes));
        if (readOnlyAttributesCfg != null) {
            List<String> configured = Arrays.asList(readOnlyAttributesCfg);
            logger.infof("Configured %s: %s", configKey, configured);
            readOnlyAttributes.addAll(configured);
        }

        String regexStr = readOnlyAttributes.stream()
                .map(configAttrName -> configAttrName.endsWith("*")
                        ? "^" + Pattern.quote(configAttrName.substring(0, configAttrName.length() - 1)) + ".*$"
                        : "^" + Pattern.quote(configAttrName ) + "$")
                .collect(Collectors.joining("|"));
        regexStr = "(?i:" + regexStr + ")";

        logger.debugf("Regex used for %s: %s", configKey, regexStr);
        return Pattern.compile(regexStr);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }
    public static final String PROVIDER_ID = "legacy-user-profile";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


}
