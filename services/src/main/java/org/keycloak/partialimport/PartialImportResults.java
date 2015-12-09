/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.partialimport;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.UriInfo;
import org.keycloak.events.admin.OperationType;
import org.keycloak.services.resources.admin.AdminEventBuilder;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class PartialImportResults {

    private final Set<PartialImportResult> importResults = new HashSet<>();

    public void addResult(PartialImportResult result) {
        System.out.println("PartialImportResults: add " + result.getResourceName() + " action=" + result.getAction());
        importResults.add(result);
    }

    public void addAllResults(PartialImportResults results) {
        importResults.addAll(results.getResults());
    }

    public int getAdded() {
        int added = 0;
        for (PartialImportResult result : importResults) {
            if (result.getAction() == Action.ADDED) added++;
        }

        return added;
    }

    public int getOverwritten() {
        int overwritten = 0;
        for (PartialImportResult result : importResults) {
            System.out.println("action=" + result.getAction());
            if (result.getAction() == Action.OVERWRITTEN) overwritten++;
        }

        return overwritten;
    }

    public int getSkipped() {
        int skipped = 0;
        for (PartialImportResult result : importResults) {
            if (result.getAction() == Action.SKIPPED) skipped++;
        }

        return skipped;
    }

    public Set<PartialImportResult> getResults() {
        return importResults;
    }
}
