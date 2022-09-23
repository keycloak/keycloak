/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.console.page.clients.authorization.scope;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.WebElement;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopesTable extends DataTable {

    public ScopeRepresentation findByName(String name) {
        search(name);
        List<ScopeRepresentation> result = getTableRows();
        if (result.isEmpty()) {
            return null;
        } else {
            assert 1 == result.size();
            return result.get(0);
        }
    }

    public boolean contains(String name) {
        for (ScopeRepresentation representation : getTableRows()) {
            if (name.equals(representation.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<ScopeRepresentation> getTableRows() {
        List<ScopeRepresentation> rows = new ArrayList<>();
        for (WebElement row : rows()) {
            ScopeRepresentation representation = toRepresentation(row);
            if (representation != null) {
                rows.add(representation);
            }
        }
        return rows;
    }

    public ScopeRepresentation toRepresentation(WebElement row) {
        ScopeRepresentation representation = null;
        List<WebElement> tds = row.findElements(tagName("td"));
        if (!(tds.isEmpty() || getTextFromElement(tds.get(1)).isEmpty())) {
            representation = new ScopeRepresentation();
            representation.setName(getTextFromElement(tds.get(1)));
        }
        return representation;
    }
}
