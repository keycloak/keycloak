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

package org.keycloak.userprofile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;

/**
 * Registers Declarative User Profile attributes for per-client "last login" tracking, so the
 * timestamps are displayed with proper labels in the admin console.
 * <p>
 * Attribute names and format (ISO-8601 {@link java.time.OffsetDateTime}, UTC) intentionally match
 * those previously written by the community keycloak-auditor SPI plugin
 * ({@code aud_usr_last-login_<clientId>} on the user, {@code aud_cls_last-login} on the client),
 * so deployments already using that plugin keep their existing per-client login history without
 * any conversion when they switch to this native implementation. The single global per-user
 * timestamp is tracked separately as {@link org.keycloak.models.UserModel#getLastSuccessfulAuthentication()},
 * a dedicated field rather than an attribute; see {@code LastSuccessfulAuthenticationMigrationResource}
 * for migrating a plugin-written {@code aud_usr_last-login} attribute into that field.
 */
public class LoginActivityAttributeRegistrar {

    private static final Logger logger = Logger.getLogger(LoginActivityAttributeRegistrar.class);

    public static final String GROUP_NAME = "audit";
    private static final String GROUP_HEADER = "Audit Information";
    private static final String ADMIN_ROLE = UserProfileConstants.ROLE_ADMIN;

    private static final String PER_CLIENT_ATTRIBUTE_PREFIX = "aud_usr_last-login";
    public static final String CLIENT_LAST_LOGIN_ATTRIBUTE = "aud_cls_last-login";

    /**
     * Matches {@code UPConfigUtils.ATTRIBUTE_NAME_PATTERN} ({@code [a-zA-Z0-9._-]+}). Client IDs that
     * are themselves valid user-profile attribute name characters (the common case) are embedded
     * as-is, preserving exact attribute-key compatibility with the community keycloak-auditor plugin.
     * Client IDs that aren't (e.g. SAML entity IDs/URLs containing ':' or '/') would otherwise produce
     * an invalid attribute name and make {@code UPConfigUtils.validate} reject the whole user profile
     * configuration, breaking logins for every client in the realm; those are sanitized and given a
     * collision-safe hash suffix instead.
     */
    private static final Pattern SAFE_ATTRIBUTE_NAME_CHARS = Pattern.compile("[a-zA-Z0-9._-]+");
    private static final Pattern UNSAFE_CHAR = Pattern.compile("[^a-zA-Z0-9._-]");

    /**
     * The global last-login attribute written by the community keycloak-auditor SPI plugin
     * ({@code aud_usr_last-login}, an ISO-8601 {@link java.time.OffsetDateTime} string). Core no
     * longer writes this attribute — the equivalent data lives in
     * {@link org.keycloak.models.UserModel#getLastSuccessfulAuthentication()} — but it is still
     * read by the one-off migration endpoint that backfills that field for existing plugin users.
     */
    public static final String LEGACY_GLOBAL_LAST_LOGIN_ATTRIBUTE = "aud_usr_last-login";

    private LoginActivityAttributeRegistrar() {
    }

    public static String perClientAttributeName(String clientId) {
        if (SAFE_ATTRIBUTE_NAME_CHARS.matcher(clientId).matches()) {
            return PER_CLIENT_ATTRIBUTE_PREFIX + "_" + clientId;
        }
        return PER_CLIENT_ATTRIBUTE_PREFIX + "_" + UNSAFE_CHAR.matcher(clientId).replaceAll("_") + "_" + sha256Hex8(clientId);
    }

    private static String sha256Hex8(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Ensures a per-client last-login attribute exists in the user profile configuration for the
     * given realm. Idempotent; only writes the configuration back if something actually changed.
     */
    public static void registerClientLoginAttribute(KeycloakSession session, RealmModel realm, String clientId) {
        session.getContext().setRealm(realm);
        UserProfileProvider upp = session.getProvider(UserProfileProvider.class);
        UPConfig config = upp.getConfiguration();

        String attrName = perClientAttributeName(clientId);
        boolean modified = ensureGroup(config);
        modified |= ensureAttribute(config, attrName, "Last Login (" + clientId + ")");

        if (modified) {
            logger.infof("Registered last-login user profile attribute '%s' for realm '%s'", attrName, realm.getName());
            upp.setConfiguration(config);
        }
    }

    private static boolean ensureGroup(UPConfig config) {
        if (config.getGroups() != null
                && config.getGroups().stream().anyMatch(g -> GROUP_NAME.equals(g.getName()))) {
            return false;
        }

        UPGroup group = new UPGroup();
        group.setName(GROUP_NAME);
        group.setDisplayHeader(GROUP_HEADER);
        config.addGroup(group);
        return true;
    }

    private static boolean ensureAttribute(UPConfig config, String name, String displayName) {
        if (config.getAttributes() != null) {
            boolean exists = config.getAttributes().stream().anyMatch(a -> name.equals(a.getName()));
            if (exists) {
                return false;
            }
        }

        UPAttribute attribute = new UPAttribute();
        attribute.setName(name);
        attribute.setDisplayName(displayName);
        attribute.setGroup(GROUP_NAME);
        attribute.setAnnotations(new HashMap<>());

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setView(new HashSet<>(Set.of(ADMIN_ROLE)));
        permissions.setEdit(new HashSet<>());
        attribute.setPermissions(permissions);

        if (config.getAttributes() == null) {
            config.setAttributes(new ArrayList<>());
        }
        config.getAttributes().add(attribute);
        return true;
    }
}
