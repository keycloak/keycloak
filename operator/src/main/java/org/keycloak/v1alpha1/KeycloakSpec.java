package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"extensions","external","externalAccess","externalDatabase","instances","keycloakDeploymentSpec","migration","multiAvailablityZones","podDisruptionBudget","postgresDeploymentSpec","profile","storageClassName","unmanaged"})
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
public class KeycloakSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("extensions")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of extensions, where each one is a URL to a JAR files that will be deployed in Keycloak.")
    private java.util.List<String> extensions;

    public java.util.List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(java.util.List<String> extensions) {
        this.extensions = extensions;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("external")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Contains configuration for external Keycloak instances. Unmanaged needs to be set to true to use this.")
    private ExternalSpec external;

    public ExternalSpec getExternal() {
        return external;
    }

    public void setExternal(ExternalSpec external) {
        this.external = external;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("externalAccess")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Controls external Ingress/Route settings.")
    private ExternalAccessSpec externalAccess;

    public ExternalAccessSpec getExternalAccess() {
        return externalAccess;
    }

    public void setExternalAccess(ExternalAccessSpec externalAccess) {
        this.externalAccess = externalAccess;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("externalDatabase")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Controls external database settings. Using an external database requires providing a secret containing credentials as well as connection details. Here's an example of such secret: \n     apiVersion: v1     kind: Secret     metadata:         name: keycloak-db-secret         namespace: keycloak     stringData:         POSTGRES_DATABASE: <Database Name>         POSTGRES_EXTERNAL_ADDRESS: <External Database IP or URL (resolvable by K8s)>         POSTGRES_EXTERNAL_PORT: <External Database Port>         # Strongly recommended to use <'Keycloak CR Name'-postgresql>         POSTGRES_HOST: <Database Service Name>         POSTGRES_PASSWORD: <Database Password>         # Required for AWS Backup functionality         POSTGRES_SUPERUSER: true         POSTGRES_USERNAME: <Database Username>      type: Opaque \n Both POSTGRES_EXTERNAL_ADDRESS and POSTGRES_EXTERNAL_PORT are specifically required for creating connection to the external database. The secret name is created using the following convention:       <Custom Resource Name>-db-secret \n For more information, please refer to the Operator documentation.")
    private ExternalDatabaseSpec externalDatabase;

    public ExternalDatabaseSpec getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(ExternalDatabaseSpec externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("instances")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Number of Keycloak instances in HA mode. Default is 1.")
    private Long instances;

    public Long getInstances() {
        return instances;
    }

    public void setInstances(Long instances) {
        this.instances = instances;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("keycloakDeploymentSpec")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Resources (Requests and Limits) for KeycloakDeployment.")
    private KeycloakDeploymentSpecSpec keycloakDeploymentSpec;

    public KeycloakDeploymentSpecSpec getKeycloakDeploymentSpec() {
        return keycloakDeploymentSpec;
    }

    public void setKeycloakDeploymentSpec(KeycloakDeploymentSpecSpec keycloakDeploymentSpec) {
        this.keycloakDeploymentSpec = keycloakDeploymentSpec;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("migration")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Specify Migration configuration")
    private MigrationSpec migration;

    public MigrationSpec getMigration() {
        return migration;
    }

    public void setMigration(MigrationSpec migration) {
        this.migration = migration;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("multiAvailablityZones")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Specify PodAntiAffinity settings for Keycloak deployment in Multi AZ")
    private MultiAvailablityZonesSpec multiAvailablityZones;

    public MultiAvailablityZonesSpec getMultiAvailablityZones() {
        return multiAvailablityZones;
    }

    public void setMultiAvailablityZones(MultiAvailablityZonesSpec multiAvailablityZones) {
        this.multiAvailablityZones = multiAvailablityZones;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("podDisruptionBudget")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Specify PodDisruptionBudget configuration.")
    private PodDisruptionBudgetSpec podDisruptionBudget;

    public PodDisruptionBudgetSpec getPodDisruptionBudget() {
        return podDisruptionBudget;
    }

    public void setPodDisruptionBudget(PodDisruptionBudgetSpec podDisruptionBudget) {
        this.podDisruptionBudget = podDisruptionBudget;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("postgresDeploymentSpec")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Resources (Requests and Limits) for PostgresDeployment.")
    private PostgresDeploymentSpecSpec postgresDeploymentSpec;

    public PostgresDeploymentSpecSpec getPostgresDeploymentSpec() {
        return postgresDeploymentSpec;
    }

    public void setPostgresDeploymentSpec(PostgresDeploymentSpecSpec postgresDeploymentSpec) {
        this.postgresDeploymentSpec = postgresDeploymentSpec;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("profile")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Profile used for controlling Operator behavior. Default is empty.")
    private String profile;

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("storageClassName")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Name of the StorageClass for Postgresql Persistent Volume Claim")
    private String storageClassName;

    public String getStorageClassName() {
        return storageClassName;
    }

    public void setStorageClassName(String storageClassName) {
        this.storageClassName = storageClassName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("unmanaged")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("When set to true, this Keycloak will be marked as unmanaged and will not be managed by this operator. It can then be used for targeting purposes.")
    private Boolean unmanaged;

    public Boolean getUnmanaged() {
        return unmanaged;
    }

    public void setUnmanaged(Boolean unmanaged) {
        this.unmanaged = unmanaged;
    }
}
