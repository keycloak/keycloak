package org.keycloak.quarkus.runtime.services.metrics;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;

@Singleton
public class HttpClientMeterFilter implements MeterFilter {
    @Override
    public Meter.Id map(Meter.Id id) {
        if (id.getName().startsWith("httpcomponents.httpclient.")) {
            // Replace the httpcomponents.httpclient prefix with one that is implementation independent
            return id.withName(id.getName().replace("httpcomponents.httpclient.", "http.client."));
        }
        return id;
    }
}
