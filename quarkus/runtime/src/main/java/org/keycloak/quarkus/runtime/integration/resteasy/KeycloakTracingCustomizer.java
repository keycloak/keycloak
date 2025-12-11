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

package org.keycloak.quarkus.runtime.integration.resteasy;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.CDI;

import org.keycloak.common.Version;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public final class KeycloakTracingCustomizer implements HandlerChainCustomizer {

    private static class StartHandler implements ServerRestHandler {
        private final String className;
        private final String methodName;
        private final String spanName;

        public StartHandler(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            this.spanName = StringUtils.substringAfterLast(className, ".") + "." + methodName;
        }

        @Override
        public void handle(ResteasyReactiveRequestContext requestContext) {
            if (requestContext.getProperty("span") != null) {
                return;
            }
            OpenTelemetry openTelemetry = CDI.current().select(OpenTelemetry.class).get();
            Tracer myTracer = openTelemetry.getTracer(this.getClass().getName(), Version.VERSION);
            SpanBuilder spanBuilder = myTracer.spanBuilder(spanName);
            spanBuilder.setParent(Context.current().with(Span.current()));
            // for semconv >= 1.32 use CODE_FUNCTION_NAME instead
            spanBuilder.setAttribute(CodeIncubatingAttributes.CODE_FUNCTION, methodName);
            spanBuilder.setAttribute(CodeIncubatingAttributes.CODE_NAMESPACE, className);
            Span span = spanBuilder.startSpan();
            requestContext.setProperty("span", span);
            requestContext.setProperty("scope", span.makeCurrent());
        }
    }

    public static class EndHandler implements ServerRestHandler {
        @Override
        public void handle(ResteasyReactiveRequestContext requestContext) {
            Scope scope = (Scope) requestContext.getProperty("scope");
            if (scope != null) {
                scope.close();
                requestContext.removeProperty("scope");
            }
            Span span = (Span) requestContext.getProperty("span");
            if (span != null) {
                span.end();
                requestContext.removeProperty("span");
            }
        }
    }

    @Override
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
            ServerResourceMethod resourceMethod) {
        List<ServerRestHandler> handlers = new ArrayList<>();

        switch (phase) {
            case BEFORE_METHOD_INVOKE:
                handlers.add(new StartHandler(resourceClass.getClassName(), resourceMethod.getName()));
                break;
            case AFTER_METHOD_INVOKE:
                handlers.add(new EndHandler());
                break;
        }

        return handlers;
    }
}
