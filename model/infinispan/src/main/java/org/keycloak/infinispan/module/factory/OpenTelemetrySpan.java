package org.keycloak.infinispan.module.factory;

import java.util.Objects;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.infinispan.telemetry.InfinispanSpan;
import org.infinispan.telemetry.SafeAutoClosable;

public class OpenTelemetrySpan<T> implements InfinispanSpan<T> {

    private final Span span;

    public OpenTelemetrySpan(Span span) {
        this.span = Objects.requireNonNull(span);
        // TODO: This is actually wrong if you are doing asynchronous calls, but it allows the JGroups calls to be nested
        // This should be fixed in ISPN 16+ so that it is no longer needed
        // https://github.com/infinispan/infinispan/issues/15287
        //this.scope = span.makeCurrent();
    }

    @Override
    public SafeAutoClosable makeCurrent() {
        //noinspection resource
        Scope scope = span.makeCurrent();
        return scope::close;
    }

    @Override
    public void complete() {
        span.end();
    }

    @Override
    public void recordException(Throwable throwable) {
        span.setStatus(StatusCode.ERROR, "Error during the cache request processing");
        span.recordException(throwable);
    }
}
