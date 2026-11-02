/*
 * Copyright 2026 Andrzej Świętek
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

package org.keycloak.broker.oidc.mappers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * The rows of a role table, as stored by a {@link org.keycloak.provider.ProviderConfigProperty#MAP_TYPE} property.
 */
final class RoleTableRows {

    /**
     * @param key   the field value
     * @param value the role reference
     */
    record Row(String key, String value) {
    }

    private static final TypeReference<List<Row>> ROWS = new TypeReference<>() {
    };

    private RoleTableRows() {
    }

    /**
     * The table with {@code rewriteReference} applied to each role reference, keeping the rows in order; empty when no
     * reference changes, or the table cannot be read or has a row without a field value or role reference.
     */
    static Optional<String> rewritten(String table, UnaryOperator<String> rewriteReference) {
        return read(table).flatMap(rows -> rewrittenRows(rows, rewriteReference));
    }

    private static Optional<String> rewrittenRows(List<Row> rows, UnaryOperator<String> rewriteReference) {
        List<Row> newRows = rows.stream().map(row -> rewrittenRow(row, rewriteReference)).toList();
        return newRows.equals(rows) ? Optional.empty() : write(newRows);
    }

    private static Row rewrittenRow(Row row, UnaryOperator<String> rewriteReference) {
        return new Row(row.key(), rewriteReference.apply(row.value()));
    }

    private static Optional<List<Row>> read(String table) {
        return parse(table).filter(rows -> rows.stream().allMatch(RoleTableRows::isComplete));
    }

    private static boolean isComplete(Row row) {
        return row != null && row.key() != null && row.value() != null;
    }

    private static Optional<List<Row>> parse(String table) {
        try {
            return Optional.ofNullable(JsonSerialization.readValue(table, ROWS));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> write(List<Row> rows) {
        try {
            return Optional.of(JsonSerialization.writeValueAsString(rows));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
