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
package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawParameterizedSqlStatement;

/**
 *
 * @author rmartinc
 */
public class JpaUpdate26_4_0_SamlEncryptionAttributes extends CustomKeycloakTask {

    @Override
    protected String getTaskId() {
        return "Insert legacy encryption attributes in SAML clients";
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(createInsertQueryForAttribute("saml.encryption.algorithm", "http://www.w3.org/2001/04/xmlenc#aes128-cbc"));
        statements.add(createInsertQueryForAttribute("saml.encryption.keyAlgorithm", "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"));
        statements.add(createInsertQueryForAttribute("saml.encryption.digestMethod", "http://www.w3.org/2000/09/xmldsig#sha1"));
    }

    private SqlStatement createInsertQueryForAttribute(String attribute, String value) {
        final String clientTable = getTableName("CLIENT");
        final String clientAttributesTable = getTableName("CLIENT_ATTRIBUTES");
        return new RawParameterizedSqlStatement(
                "INSERT INTO " + clientAttributesTable + " (CLIENT_ID,NAME,VALUE) " +
                "SELECT ID, ?, ? FROM " + clientTable + " WHERE PROTOCOL = ? AND ID NOT IN " +
                "(SELECT CLIENT_ID FROM " + clientAttributesTable + " WHERE NAME = ?)",
                attribute, value, "saml", attribute
        );
    }
}
