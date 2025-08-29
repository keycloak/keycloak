package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Default;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class ServiceMonitorSpec {

    public static final String DEFAULT_INTERVAL = "30s";
    public static final String DEFAULT_SCRAPE_TIMEOUT = "10s";

    @JsonPropertyDescription("Enables or disables the creation of the ServiceMonitor.")
    @Default("true")
    private boolean enabled = true;

    @JsonPropertyDescription("Interval at which metrics should be scraped")
    @Default(DEFAULT_INTERVAL)
    private String interval = DEFAULT_INTERVAL;

    @JsonPropertyDescription("Timeout after which the scrape is ended")
    @Default(DEFAULT_SCRAPE_TIMEOUT)
    private String scrapeTimeout = DEFAULT_SCRAPE_TIMEOUT;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getScrapeTimeout() {
        return scrapeTimeout;
    }

    public void setScrapeTimeout(String scrapeTimeout) {
        this.scrapeTimeout = scrapeTimeout;
    }

    public static ServiceMonitorSpec get(Keycloak keycloak) {
        return CRDUtils.keycloakSpecOf(keycloak)
              .map(KeycloakSpec::getServiceMonitorSpec)
              .orElse(new ServiceMonitorSpec());
    }
}
