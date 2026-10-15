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

package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.keycloak.common.util.Base64Url;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.RawParameterizedSqlStatement;
import liquibase.structure.core.Table;

/**
 * Stores the current key size of aes-generated key providers that were created before the secretSize default changed
 * in 26.7.0 and carry no secretSize configuration. Without it the admin console shows the new default for such a
 * provider, and saving it without any change rotates the realm AES key (see https://github.com/keycloak/keycloak/issues/54016).
 */
public class JpaUpdate26_8_0_AesGeneratedSecretSize extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String componentTable = database.correctObjectName("COMPONENT", Table.class);
        String componentConfigTable = database.correctObjectName("COMPONENT_CONFIG", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                     "SELECT c.ID, cc.VALUE FROM " + getTableName(componentTable) + " c"
                     + " JOIN " + getTableName(componentConfigTable) + " cc ON cc.COMPONENT_ID = c.ID"
                     + " WHERE c.PROVIDER_ID = 'aes-generated' AND cc.NAME = 'secret'"
                     + " AND NOT EXISTS (SELECT 1 FROM " + getTableName(componentConfigTable) + " ss"
                     + " WHERE ss.COMPONENT_ID = c.ID AND ss.NAME = 'secretSize')");
             ResultSet rs = ps.executeQuery()) {

            int stored = 0;
            // ComponentModel config is multivalued, although the generated secret is always a single value; guard
            // against writing more than one secretSize entry per component anyway.
            Set<String> sizedComponents = new HashSet<>();
            while (rs.next()) {
                String componentId = rs.getString(1);
                if (!sizedComponents.add(componentId)) {
                    continue;
                }
                String secret = rs.getString(2);
                // The secret is written by AbstractGeneratedSecretKeyProviderFactory as unpadded base64url,
                // so decoding it yields the exact number of key bytes the provider was generated with.
                int size = Base64Url.decode(secret).length;
                statements.add(new RawParameterizedSqlStatement(
                        "INSERT INTO " + getTableName(componentConfigTable) + " (ID, COMPONENT_ID, NAME, VALUE) VALUES (?, ?, 'secretSize', ?)",
                        UUID.randomUUID().toString(), componentId, String.valueOf(size)));
                stored++;
            }
            confirmationMessage.append("Stored the key size of " + stored + " aes-generated key provider(s) that had no size configured.");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when storing the key size of aes-generated providers", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Store the key size of aes-generated key providers without a configured size";
    }
}
