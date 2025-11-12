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

package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Default;
import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;
import org.keycloak.operator.update.UpdateStrategy;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
@ValidationRule(
        value = "self.strategy != 'Explicit' || has(self.revision)",
        message = "The 'revision' field is required when 'Explicit' strategy is used"
)
public class UpdateSpec {

    // those are the default, keep them in sync.
    private static final UpdateStrategy DEFAULT = UpdateStrategy.RECREATE_ON_IMAGE_CHANGE;
    private static final String DEFAULT_JSON = "RecreateOnImageChange";

    @JsonProperty("scheduling")
    @JsonPropertyDescription("In this section you can configure the update job's scheduling")
    private SchedulingSpec schedulingSpec;

    @JsonPropertyDescription("Sets the update strategy to use.")
    @Default(DEFAULT_JSON)
    private UpdateStrategy strategy;

    @JsonPropertyDescription("When use the Explicit strategy, the revision signals if a rolling update can be used or not.")
    private String revision;

    @JsonProperty("labels")
    @JsonPropertyDescription("Optionally set to add additional labels to the Job created for the update.")
    Map<String, String> labels = new LinkedHashMap<String, String>();

    public UpdateStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(UpdateStrategy strategy) {
        this.strategy = strategy;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public SchedulingSpec getSchedulingSpec() {
        return schedulingSpec;
    }

    public void setSchedulingSpec(SchedulingSpec schedulingSpec) {
        this.schedulingSpec = schedulingSpec;
    }

    public static UpdateStrategy getUpdateStrategy(Keycloak keycloak) {
        return CRDUtils.keycloakSpecOf(keycloak)
                .map(KeycloakSpec::getUpdateSpec)
                .map(UpdateSpec::getStrategy)
                .orElse(DEFAULT);
    }

    public static Optional<String> getRevision(Keycloak keycloak) {
        return CRDUtils.keycloakSpecOf(keycloak)
                .map(KeycloakSpec::getUpdateSpec)
                .map(UpdateSpec::getRevision);
    }
    
    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }
}
