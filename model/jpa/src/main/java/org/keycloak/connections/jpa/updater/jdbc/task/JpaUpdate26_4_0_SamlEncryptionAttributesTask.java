/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_4_0_SamlEncryptionAttributes}.
 * Inserts default SAML encryption attributes for all SAML clients that do not already have them.
 */
public class JpaUpdate26_4_0_SamlEncryptionAttributesTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Add default SAML encryption attributes";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String clientTable = NativeSqlHelper.qualified("CLIENT", defaultSchema);
        String clientAttributesTable = NativeSqlHelper.qualified("CLIENT_ATTRIBUTES", defaultSchema);
        List<String> stmts = new ArrayList<>();
        stmts.add(insertAttr(clientTable, clientAttributesTable, "saml.encryption.algorithm",
                "http://www.w3.org/2001/04/xmlenc#aes128-cbc"));
        stmts.add(insertAttr(clientTable, clientAttributesTable, "saml.encryption.keyAlgorithm",
                "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"));
        stmts.add(insertAttr(clientTable, clientAttributesTable, "saml.encryption.digestMethod",
                "http://www.w3.org/2000/09/xmldsig#sha1"));
        return stmts;
    }

    private static String insertAttr(String clientTable, String clientAttributesTable,
            String attribute, String value) {
        return "INSERT INTO " + clientAttributesTable + " (CLIENT_ID, NAME, VALUE) "
                + "SELECT ID, " + NativeSqlHelper.sqlLiteral(attribute) + ", " + NativeSqlHelper.sqlLiteral(value)
                + " FROM " + clientTable + " WHERE PROTOCOL = 'saml' AND ID NOT IN "
                + "(SELECT CLIENT_ID FROM " + clientAttributesTable
                + " WHERE NAME = " + NativeSqlHelper.sqlLiteral(attribute) + ")";
    }
}
