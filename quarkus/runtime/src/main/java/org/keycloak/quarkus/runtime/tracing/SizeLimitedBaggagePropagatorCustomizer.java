package org.keycloak.quarkus.runtime.tracing;

import jakarta.inject.Singleton;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;

/**
 * Wraps the {@link W3CBaggagePropagator} with a {@link SizeLimitedBaggagePropagator}
 * that enforces W3C Baggage specification size limits before delegating extraction.
 *
 * <p>Workaround for CVE-2026-45292 on OTel SDK versions that lack built-in limits (&le; 1.61.0).
 *
 * // TODO: remove this workaround when Keycloak upgrades to a Quarkus version shipping OTel >= 1.62.0
 * // see https://github.com/keycloak/keycloak/issues/49570
 *
 * @see SizeLimitedBaggagePropagator
 */
@Singleton
public class SizeLimitedBaggagePropagatorCustomizer implements TextMapPropagatorCustomizer {

    @Override
    public TextMapPropagator customize(Context context) {
        TextMapPropagator propagator = context.propagator();
        if (propagator instanceof W3CBaggagePropagator) {
            return new SizeLimitedBaggagePropagator(propagator);
        }
        return propagator;
    }
}
