package org.keycloak.operator.crds.v2alpha1.deployment;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder",
        lazyCollectionInitEnabled = false, refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.PodTemplateSpec.class)
})
public class KeycloakSpecUnsupported {

    @JsonPropertyDescription("You can configure that will be merged with the one configured by default by the operator.\n" +
            "Use at your own risk, we reserve the possibility to remove/change the way any field gets merged in future releases without notice.\n" +
            "Reference: https://kubernetes.io/docs/concepts/workloads/pods/#pod-templates")
    private PodTemplateSpec podTemplate;

    public KeycloakSpecUnsupported() {}

    public KeycloakSpecUnsupported(PodTemplateSpec podTemplate) {
        this.podTemplate = podTemplate;
    }

    public PodTemplateSpec getPodTemplate() {
        return podTemplate;
    }

    public void setPodTeplate(PodTemplateSpec podTemplate) {
        this.podTemplate = podTemplate;
    }

}
