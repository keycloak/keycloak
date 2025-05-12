package org.keycloak.infinispan.module.factory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.infinispan.telemetry.InfinispanSpan;
import org.infinispan.telemetry.InfinispanSpanAttributes;
import org.infinispan.telemetry.InfinispanSpanContext;
import org.infinispan.telemetry.InfinispanTelemetry;

public class OpenTelemetryService implements InfinispanTelemetry, TextMapGetter<InfinispanSpanContext> {

    private static final String INFINISPAN_SERVER_TRACING_NAME = "org.infinispan.server.tracing";
    private static final String INFINISPAN_SERVER_TRACING_VERSION = "1.0.0";

    private final Tracer tracer;
    private volatile String nodeName = "n/a";

    public OpenTelemetryService(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(INFINISPAN_SERVER_TRACING_NAME, INFINISPAN_SERVER_TRACING_VERSION);
    }

    @Override
    public <T> InfinispanSpan<T> startTraceRequest(String operationName, InfinispanSpanAttributes attributes) {
        // The original Infininspan implementation allows for filtering via the trace attributes. We don't support this here and instead trace everything.

        var builder = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.SERVER);
        // the parent context is inherited automatically,
        // because the parent span is created in the same process

        return createOpenTelemetrySpan(builder, attributes);
    }

    @Override
    public <T> InfinispanSpan<T> startTraceRequest(String operationName, InfinispanSpanAttributes attributes, InfinispanSpanContext context) {
        // The original Infinispan implementation allows for filtering via the trace attributes. We don't support this here and instead trace everything

        var builder = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.SERVER)
                .setParent(Context.current().with(Span.current()));

        return createOpenTelemetrySpan(builder, attributes);
    }

    @Override
    public void setNodeName(String nodeName) {
        if (nodeName != null) {
            this.nodeName = nodeName;
        }
    }

    private <T> InfinispanSpan<T> createOpenTelemetrySpan(SpanBuilder builder, InfinispanSpanAttributes attributes) {
        attributes.cacheName().ifPresent(cacheName -> builder.setAttribute("cache", cacheName));
        builder.setAttribute("category", attributes.category().toString());
        builder.setAttribute("server.address", nodeName);
        return new OpenTelemetrySpan<>(builder.startSpan());
    }

    @Override
    public Iterable<String> keys(InfinispanSpanContext ctx) {
        return ctx.keys();
    }

    @Override
    public String get(InfinispanSpanContext ctx, String key) {
        assert ctx != null;
        return ctx.getKey(key);
    }
}
