/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.credential;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.webauthn4j.data.attestation.statement.AttestationStatement;

public class AttestationStatementSerializationContainer {

    @JsonProperty("attStmt")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "fmt")
    private AttestationStatement attestationStatement;

    @JsonCreator
    public AttestationStatementSerializationContainer(@JsonProperty("attStmt") AttestationStatement attestationStatement) {
        this.attestationStatement = attestationStatement;
    }

    @JsonProperty("fmt")
    public String getFormat() {
        return attestationStatement.getFormat();
    }

    public AttestationStatement getAttestationStatement() {
        return attestationStatement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttestationStatementSerializationContainer that = (AttestationStatementSerializationContainer) o;
        return Objects.equals(attestationStatement, that.attestationStatement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attestationStatement);
    }
}
