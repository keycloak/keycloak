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
package org.keycloak.testsuite.console.page.clients.authorization.resource;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.WebElement;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcesTable extends DataTable {

    public ResourceRepresentation findByName(String name) {
        search(name);
        List<ResourceRepresentation> result = getTableRows();
        if (result.isEmpty()) {
            return null;
        } else {
            assert 1 == result.size();
            return result.get(0);
        }
    }

    public boolean contains(String name) {
        for (ResourceRepresentation representation : getTableRows()) {
            if (name.equals(representation.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<ResourceRepresentation> getTableRows() {
        List<ResourceRepresentation> rows = new ArrayList<>();
        for (WebElement row : rows()) {
            ResourceRepresentation representation = toRepresentation(row);
            if (representation != null) {
                rows.add(representation);
            }
        }
        return rows;
    }

    public ResourceRepresentation toRepresentation(WebElement row) {
        ResourceRepresentation representation = null;
        List<WebElement> tds = row.findElements(tagName("td"));
        try {
            if (!(tds.isEmpty() || getTextFromElement(tds.get(1)).isEmpty())) {
                representation = new ResourceRepresentation();
                representation.setName(getTextFromElement(tds.get(1)));
                representation.setType(getTextFromElement(tds.get(2)));
                representation.setUri(getTextFromElement(tds.get(3)));
                ResourceOwnerRepresentation owner = new ResourceOwnerRepresentation();
                owner.setName(getTextFromElement(tds.get(4)));
                representation.setOwner(owner);
            }
        } catch (IndexOutOfBoundsException cause) {
            // is empty
        }
        return representation;
    }
}
