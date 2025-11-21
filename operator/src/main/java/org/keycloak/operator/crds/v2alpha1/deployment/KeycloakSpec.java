/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator.crds.v2alpha1.deployment;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.model.annotation.SpecReplicas;

import org.keycloak.operator.crds.v2alpha1.deployment.spec.BootstrapAdminSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.CacheSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.DatabaseSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpManagementSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ImportSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.NetworkPolicySpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ProbeSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ProxySpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.SchedulingSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ServiceMonitorSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TracingSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TransactionsSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.Truststore;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeycloakSpec {

    @SpecReplicas
    @JsonPropertyDescription("Number of Keycloak instances. Default is 1.")
    private Integer instances;

    @JsonPropertyDescription("Custom Keycloak image to be used.")
    private String image;

    @JsonPropertyDescription("Set to force the behavior of the --optimized flag for the start command. If left unspecified the operator will assume custom images have already been augmented.")
    private Boolean startOptimized;

    @JsonPropertyDescription("Secret(s) that might be used when pulling an image from a private container image registry or repository.")
    private List<LocalObjectReference> imagePullSecrets = new ArrayList<LocalObjectReference>();

    @JsonPropertyDescription("Configuration of the Keycloak server.\n" +
            "expressed as a keys (reference: https://www.keycloak.org/server/all-config) and values that can be either direct values or references to secrets.")
    private List<ValueOrSecret> additionalOptions = new ArrayList<ValueOrSecret>(); // can't use Set due to a bug in Sundrio https://github.com/sundrio/sundrio/issues/316

    @JsonPropertyDescription("Environment variables for the Keycloak server.\n" +
            "Values can be either direct values or references to secrets. Use additionalOptions for first-class options rather than KC_ values here.")
    private List<ValueOrSecret> env = new ArrayList<ValueOrSecret>();

    @JsonProperty("http")
    @JsonPropertyDescription("In this section you can configure Keycloak features related to HTTP and HTTPS")
    private HttpSpec httpSpec;

    @JsonPropertyDescription(
            "In this section you can configure podTemplate advanced features, not production-ready, and not supported settings.\n" +
                    "Use at your own risk and open an issue with your use-case if you don't find an alternative way.")
    private UnsupportedSpec unsupported;

    @JsonProperty("ingress")
    @JsonPropertyDescription("The deployment is, by default, exposed through a basic ingress.\n" +
            "You can change this behaviour by setting the enabled property to false.")
    private IngressSpec ingressSpec;

    @JsonProperty("features")
    @JsonPropertyDescription("In this section you can configure Keycloak features, which should be enabled/disabled.")
    private FeatureSpec featureSpec;

    @JsonProperty("transaction")
    @JsonPropertyDescription("In this section you can find all properties related to the settings of transaction behavior.")
    private TransactionsSpec transactionsSpec;

    @JsonProperty("db")
    @JsonPropertyDescription("In this section you can find all properties related to connect to a database.")
    private DatabaseSpec databaseSpec;

    @JsonProperty("hostname")
    @JsonPropertyDescription("In this section you can configure Keycloak hostname and related properties.")
    private HostnameSpec hostnameSpec;

    @JsonPropertyDescription("In this section you can configure Keycloak truststores.")
    private Map<String, Truststore> truststores = new LinkedHashMap<>();

    @JsonProperty("cache")
    @JsonPropertyDescription("In this section you can configure Keycloak's cache")
    private CacheSpec cacheSpec;

    @JsonProperty("resources")
    @JsonPropertyDescription("Compute Resources required by Keycloak container")
    private ResourceRequirements resourceRequirements;

    @JsonProperty("proxy")
    @JsonPropertyDescription("In this section you can configure Keycloak's reverse proxy setting")
    private ProxySpec proxySpec;

    @JsonProperty("httpManagement")
    @JsonPropertyDescription("In this section you can configure Keycloak's management interface setting.")
    private HttpManagementSpec httpManagementSpec;

    @JsonProperty("scheduling")
    @JsonPropertyDescription("In this section you can configure Keycloak's scheduling")
    private SchedulingSpec schedulingSpec;

    @JsonProperty("import")
    @JsonPropertyDescription("In this section you can configure import Jobs")
    private ImportSpec importSpec;

    @JsonProperty("bootstrapAdmin")
    @JsonPropertyDescription("In this section you can configure Keycloak's bootstrap admin - will be used only for initial cluster creation.")
    private BootstrapAdminSpec bootstrapAdminSpec;

    @JsonProperty("networkPolicy")
    @JsonPropertyDescription("Controls the ingress traffic flow into Keycloak pods.")
    private NetworkPolicySpec networkPolicySpec;

    @JsonProperty("tracing")
    @JsonPropertyDescription("In this section you can configure OpenTelemetry Tracing for Keycloak.")
    private TracingSpec tracingSpec;

    @JsonProperty("update")
    @JsonPropertyDescription("Configuration related to Keycloak deployment updates.")
    private UpdateSpec updateSpec;

    @JsonProperty("readinessProbe")
    @JsonPropertyDescription("Configuration for readiness probe, by default it is 10 for periodSeconds and 3 for failureThreshold")
    private ProbeSpec readinessProbeSpec;

    @JsonProperty("livenessProbe")
    @JsonPropertyDescription("Configuration for liveness probe, by default it is 10 for periodSeconds and 3 for failureThreshold")
    private ProbeSpec livenessProbeSpec;

    @JsonProperty("startupProbe")
    @JsonPropertyDescription("Configuration for startup probe, by default it is 1 for periodSeconds and 600 for failureThreshold")
    private ProbeSpec startupProbeSpec;

    @JsonProperty("serviceMonitor")
    @JsonPropertyDescription("Configuration related to the generated ServiceMonitor")
    private ServiceMonitorSpec serviceMonitorSpec;

    @JsonProperty("automountServiceAccountToken")
    @JsonPropertyDescription("Set this to to false to disable automounting the default ServiceAccount Token and Service CA. This is enabled by default.")
    private Boolean automountServiceAccountToken;

    public HttpSpec getHttpSpec() {
        return httpSpec;
    }

    public void setHttpSpec(HttpSpec httpSpec) {
        this.httpSpec = httpSpec;
    }

    public UnsupportedSpec getUnsupported() {
        return unsupported;
    }

    public void setUnsupported(UnsupportedSpec unsupported) {
        this.unsupported = unsupported;
    }

    public FeatureSpec getFeatureSpec() {
        return featureSpec;
    }

    public void setFeatureSpec(FeatureSpec featureSpec) {
        this.featureSpec = featureSpec;
    }

    public TransactionsSpec getTransactionsSpec() {
        return transactionsSpec;
    }

    public void setTransactionsSpec(TransactionsSpec transactionsSpec) {
        this.transactionsSpec = transactionsSpec;
    }

    public IngressSpec getIngressSpec() {
        return ingressSpec;
    }

    public void setIngressSpec(IngressSpec ingressSpec) {
        this.ingressSpec = ingressSpec;
    }

    public DatabaseSpec getDatabaseSpec() {
        return databaseSpec;
    }

    public void setDatabaseSpec(DatabaseSpec databaseSpec) {
        this.databaseSpec = databaseSpec;
    }

    public HostnameSpec getHostnameSpec() {
        return hostnameSpec;
    }

    public void setHostnameSpec(HostnameSpec hostnameSpec) {
        this.hostnameSpec = hostnameSpec;
    }

    public Integer getInstances() {
        return instances;
    }

    public void setInstances(Integer instances) {
        this.instances = instances;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<LocalObjectReference> getImagePullSecrets() {
        return this.imagePullSecrets;
    }

    public void setImagePullSecrets(List<LocalObjectReference> imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
    }

    public HttpManagementSpec getHttpManagementSpec() {
        return httpManagementSpec;
    }

    public void setHttpManagementSpec(HttpManagementSpec httpManagementSpec) {
        this.httpManagementSpec = httpManagementSpec;
    }

    public List<ValueOrSecret> getAdditionalOptions() {
        if (this.additionalOptions == null) {
            this.additionalOptions = new ArrayList<>();
        }
        return additionalOptions;
    }

    public List<ValueOrSecret> getEnv() {
        return env;
    }

    public void setEnv(List<ValueOrSecret> env) {
        this.env = env;
    }

    public void setAdditionalOptions(List<ValueOrSecret> additionalOptions) {
        this.additionalOptions = additionalOptions;
    }

    public Boolean getStartOptimized() {
        return startOptimized;
    }

    public void setStartOptimized(Boolean optimized) {
        this.startOptimized = optimized;
    }

    public Map<String, Truststore> getTruststores() {
        return truststores;
    }

    public void setTruststores(Map<String, Truststore> truststores) {
        if (truststores == null) {
            truststores = new LinkedHashMap<>();
        }
        this.truststores = truststores;
    }

    public CacheSpec getCacheSpec() {
        return cacheSpec;
    }

    public void setCacheSpec(CacheSpec cache) {
        this.cacheSpec = cache;
    }

    public ResourceRequirements getResourceRequirements() {
        return resourceRequirements;
    }

    public void setResourceRequirements(ResourceRequirements resourceRequirements) {
        this.resourceRequirements = resourceRequirements;
    }

    public ProxySpec getProxySpec() {
        return proxySpec;
    }

    public void setProxySpec(ProxySpec proxySpec) {
        this.proxySpec = proxySpec;
    }

    public SchedulingSpec getSchedulingSpec() {
        return schedulingSpec;
    }

    public void setSchedulingSpec(SchedulingSpec schedulingSpec) {
        this.schedulingSpec = schedulingSpec;
    }

    public BootstrapAdminSpec getBootstrapAdminSpec() {
        return bootstrapAdminSpec;
    }

    public void setBootstrapAdminSpec(BootstrapAdminSpec bootstrapAdminSpec) {
        this.bootstrapAdminSpec = bootstrapAdminSpec;
    }

    public NetworkPolicySpec getNetworkPolicySpec() {
        return networkPolicySpec;
    }

    public void setNetworkPolicySpec(NetworkPolicySpec networkPolicySpec) {
        this.networkPolicySpec = networkPolicySpec;
    }

    public TracingSpec getTracingSpec() {
        return tracingSpec;
    }

    public void setTracingSpec(TracingSpec tracingSpec) {
        this.tracingSpec = tracingSpec;
    }

    public UpdateSpec getUpdateSpec() {
        return updateSpec;
    }

    public void setUpdateSpec(UpdateSpec updateSpec) {
        this.updateSpec = updateSpec;
    }

    public ProbeSpec getLivenessProbeSpec() {return livenessProbeSpec;}

    public void setLivenessProbeSpec(ProbeSpec livenessProbeSpec) {
        this.livenessProbeSpec = livenessProbeSpec;
    }

    public ProbeSpec getReadinessProbeSpec() {return readinessProbeSpec;}

    public void setReadinessProbeSpec(ProbeSpec readinessProbeSpec) {
        this.readinessProbeSpec = readinessProbeSpec;
    }

    public ProbeSpec getStartupProbeSpec() {return startupProbeSpec;}

    public void setStartupProbeSpec(ProbeSpec startupProbeSpec) {
        this.startupProbeSpec = startupProbeSpec;
    }

    public ImportSpec getImportSpec() {
        return importSpec;
    }

    public void setImportSpec(ImportSpec importSpec) {
        this.importSpec = importSpec;
    }


    public ServiceMonitorSpec getServiceMonitorSpec() {
        return serviceMonitorSpec;
    }

    public void setServiceMonitorSpec(ServiceMonitorSpec serviceMonitorSpec) {
        this.serviceMonitorSpec = serviceMonitorSpec;
    }

    public Boolean getAutomountServiceAccountToken() {
        return automountServiceAccountToken;
    }

    public void setAutomountServiceAccountToken(Boolean automountServiceAccountToken) {
        this.automountServiceAccountToken = automountServiceAccountToken;
    }
}
