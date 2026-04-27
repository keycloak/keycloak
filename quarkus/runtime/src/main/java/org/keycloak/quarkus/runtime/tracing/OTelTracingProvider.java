/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.tracing;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Function;

import org.keycloak.tracing.TracingProvider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.semconv.ExceptionAttributes;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jboss.logging.Logger;

/**
 * Tracing provider leverages OpenTelemetry Tracing
 */
public class OTelTracingProvider implements TracingProvider {
    private static final Logger log = Logger.getLogger(OTelTracingProvider.class);
    private final OpenTelemetry openTelemetry;
    private final Deque<Scope> scopes;

    public OTelTracingProvider(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.scopes = new ConcurrentLinkedDeque<>();
    }

    OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    @Override
    public Span getCurrentSpan() {
        return Span.current();
    }

    @Override
    public Span startSpan(String tracerName, String spanName) {
        var tracer = getTracer(tracerName);
        return startSpan(tracer.spanBuilder(spanName));
    }

    @Override
    public Span startSpan(SpanBuilder builder) {
        var currentSpan = builder.startSpan();
        scopes.push(currentSpan.makeCurrent());

        if (log.isDebugEnabled()) {
            log.debugf("Start span '%s' (spanId: '%s')", getSpanName(currentSpan), currentSpan.getSpanContext().getSpanId());
        }

        return currentSpan;
    }

    @Override
    public void endSpan() {
        if (scopes.isEmpty()) {
            log.info("All spans during tracing already ended. Verify that 'TracingProvider.endSpan()' method was not executed multiple times in the span hierarchy.");
            return;
        }

        var span = getCurrentSpan();
        if (span != null) {
            if (log.isDebugEnabled()) {
                log.debugf("End span '%s' (spanId: '%s')", getSpanName(span), span.getSpanContext().getSpanId());
            }
            span.end();
        }

        var scope = scopes.pop();
        if (scope != null) {
            scope.close();
        }
    }

    @Override
    public void error(Throwable exception) {
        var span = getCurrentSpan();
        var exceptionAttributes = Attributes.builder() // based on OTel Semantic Conventions
                .put(AttributeKey.booleanKey("exception.escaped"), true) // remove once semconv >= 1.32 is used
                .put(ExceptionAttributes.EXCEPTION_MESSAGE, exception.getMessage())
                .put(ExceptionAttributes.EXCEPTION_TYPE, exception.getClass().getCanonicalName())
                .put(ExceptionAttributes.EXCEPTION_STACKTRACE, ExceptionUtils.getStackTrace(exception))
                .build();

        span.recordException(exception, exceptionAttributes);
        span.setStatus(StatusCode.ERROR, exception.getMessage());
    }

    @Override
    public void trace(String tracerName, String spanName, Consumer<Span> execution) {
        startSpan(tracerName, spanName);
        try {
            execution.accept(getCurrentSpan());
        } catch (Throwable e) {
            error(e);
            throw e;
        } finally {
            endSpan();
        }
    }

    @Override
    public <T> T trace(String tracerName, String spanName, Function<Span, T> execution) {
        startSpan(tracerName, spanName);
        try {
            return execution.apply(getCurrentSpan());
        } catch (Throwable e) {
            error(e);
            throw e;
        } finally {
            endSpan();
        }
    }

    @Override
    public Tracer getTracer(String name, String scopeVersion) {
        return openTelemetry.getTracer(name, scopeVersion);
    }

    @Override
    public boolean validateAllSpansEnded() {
        var allEnded = scopes.isEmpty();
        if (!allEnded) {
            log.warn("Some spans during tracing were not ended. It may lead to memory leaks and incorrect scoping. Check the hierarchy of nested spans.");
        }

        return allEnded;
    }

    @Override
    public void close() {

    }

    protected static String getSpanName(Span span) {
        return span instanceof ReadableSpan rs ? rs.getName() : "unknown";
    }
}
