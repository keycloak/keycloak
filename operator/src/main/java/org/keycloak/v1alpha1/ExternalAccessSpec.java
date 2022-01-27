package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"enabled","host","tlsTermination"})
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
@lombok.ToString()
@lombok.EqualsAndHashCode()
@lombok.Setter()
@lombok.experimental.Accessors(prefix = {
    "_",
    ""
})
@io.sundr.builder.annotations.Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false, builderPackage = "io.fabric8.kubernetes.api.builder", refs = {
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ObjectReference.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.LabelSelector.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.Container.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.EnvVar.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ContainerPort.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.Volume.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.VolumeMount.class)
})
public class ExternalAccessSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("enabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("If set to true, the Operator will create an Ingress or a Route pointing to Keycloak.")
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("host")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("If set, the Operator will use value of host for Ingress host instead of default value keycloak.local. Using this setting in OpenShift environment will result an error. Only users with special permissions are allowed to modify the hostname.")
    private String host;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("tlsTermination")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("TLS Termination type for the external access. Setting this field to \"reencrypt\" will terminate TLS on the Ingress/Route level. Setting this field to \"passthrough\" will send encrypted traffic to the Pod. If unspecified, defaults to \"reencrypt\". Note, that this setting has no effect on Ingress as Ingress TLS settings are not reconciled by this operator. In other words, Ingress TLS configuration is the same in both cases and it is up to the user to configure TLS section of the Ingress.")
    private String tlsTermination;

    public String getTlsTermination() {
        return tlsTermination;
    }

    public void setTlsTermination(String tlsTermination) {
        this.tlsTermination = tlsTermination;
    }
}
