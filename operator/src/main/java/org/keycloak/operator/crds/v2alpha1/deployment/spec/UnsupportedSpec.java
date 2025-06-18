/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder",
        lazyCollectionInitEnabled = false, refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.PodTemplateSpec.class)
})
public class UnsupportedSpec {

    @JsonPropertyDescription("""
            You can configure that will be merged with the one configured by default by the operator.
            Use at your own risk, we reserve the possibility to remove/change the way any field gets merged in future releases without notice.
            Reference: https://kubernetes.io/docs/concepts/workloads/pods/#pod-templates""")
    private PodTemplateSpec podTemplate;

    public UnsupportedSpec() {}

    public UnsupportedSpec(PodTemplateSpec podTemplate) {
        this.podTemplate = podTemplate;
    }

    public PodTemplateSpec getPodTemplate() {
        return podTemplate;
    }

    public void setPodTemplate(PodTemplateSpec podTemplate) {
        this.podTemplate = podTemplate;
    }

}
