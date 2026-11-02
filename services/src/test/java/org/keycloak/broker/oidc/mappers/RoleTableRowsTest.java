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

import org.keycloak.broker.oidc.mappers.RoleTableRows.Row;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RoleTableRowsTest {

    private static String renameViewer(String reference) {
        return "viewer".equals(reference) ? "reader" : reference;
    }

    private static List<Row> rows(String table) {
        try {
            return JsonSerialization.readValue(table, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void changedReferencesAreRewrittenInOrder() {
        Optional<String> rewritten = RoleTableRows.rewritten(
                "[{\"key\":\"a\",\"value\":\"viewer\"},{\"key\":\"b\",\"value\":\"admin\"}]", RoleTableRowsTest::renameViewer);

        assertThat(rewritten.map(RoleTableRowsTest::rows),
                equalTo(Optional.of(List.of(new Row("a", "reader"), new Row("b", "admin")))));
    }

    @Test
    public void tableWithoutChangedReferenceIsKept() {
        assertThat(RoleTableRows.rewritten("[{\"key\":\"b\",\"value\":\"admin\"}]", RoleTableRowsTest::renameViewer),
                equalTo(Optional.empty()));
    }

    @Test
    public void tableWithIncompleteRowIsLeftUntouched() {
        assertThat(RoleTableRows.rewritten("[null]", RoleTableRowsTest::renameViewer), equalTo(Optional.empty()));
        assertThat(RoleTableRows.rewritten("[{\"key\":\"a\",\"value\":\"viewer\"},null]", RoleTableRowsTest::renameViewer),
                equalTo(Optional.empty()));
        assertThat(RoleTableRows.rewritten("[{\"key\":\"a\"},{\"key\":\"b\",\"value\":\"viewer\"}]", RoleTableRowsTest::renameViewer),
                equalTo(Optional.empty()));
        assertThat(RoleTableRows.rewritten("{\"key\":\"a\"}", RoleTableRowsTest::renameViewer), equalTo(Optional.empty()));
    }
}
