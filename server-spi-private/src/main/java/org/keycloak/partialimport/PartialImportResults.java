/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.partialimport;

import java.util.HashSet;
import java.util.Set;

/**
 * Aggregates all the PartialImportResult objects.
 * These results are used in the admin UI and for creating admin events.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class PartialImportResults {

    // these fields used only for marsalling from JSON with admin client
    // they are never directly set
    private int overwritten;
    private int added;
    private int skipped;

    private String errorMessage;

    private final Set<PartialImportResult> importResults = new HashSet<>();

    public void addResult(PartialImportResult result) {
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
