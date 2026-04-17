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

package org.keycloak.representations.idm;

import java.util.List;

public class EventHookProviderRepresentation {

    private String id;
    private boolean supportsBatch;
    private boolean supportsRetry;
    private boolean supportsAggregation;
    private List<ConfigPropertyRepresentation> configMetadata;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isSupportsBatch() {
        return supportsBatch;
    }

    public void setSupportsBatch(boolean supportsBatch) {
        this.supportsBatch = supportsBatch;
    }

    public boolean isSupportsRetry() {
        return supportsRetry;
    }

    public void setSupportsRetry(boolean supportsRetry) {
        this.supportsRetry = supportsRetry;
    }

    public boolean isSupportsAggregation() {
        return supportsAggregation;
    }

    public void setSupportsAggregation(boolean supportsAggregation) {
        this.supportsAggregation = supportsAggregation;
    }

    public List<ConfigPropertyRepresentation> getConfigMetadata() {
        return configMetadata;
    }

    public void setConfigMetadata(List<ConfigPropertyRepresentation> configMetadata) {
        this.configMetadata = configMetadata;
    }
}
