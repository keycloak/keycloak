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

package org.keycloak.tracing;

import java.util.function.Consumer;
import java.util.function.Function;

import org.keycloak.common.Version;
import org.keycloak.provider.Provider;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

public interface TracingProvider extends Provider {

    /**
     * Returns the {@link Span} from the current {@link Context}, falling back to a default, no-op
     * {@link Span} if there is no span in the current context.
     */
    Span getCurrentSpan();

    /**
     * Gets or creates a tracer instance, and starts a new {@link Span}.
     *
     * <p>{@link Span#end()} <b>must</b> be manually called to end this {@code Span}, or use {@link TracingProvider#trace(String, String, Consumer)} that handles it all
     *
     * <p>Example of usage:
     *
     * <pre>{@code
     * class MyClass {
     *   private final TracingProvider tracing;
     *
     *   MyClass(KeycloakSession session) {
     *     tracing = session.getProvider(TracingProvider.class);
     *   }
     *
     *   void doWork() {
     *     tracing.startSpan("MyTracerName", "mySpanName");
     *     try {
     *       doSomeWork();
     *     } finally {
     *       // To make sure we end the span even in case of an exception.
     *       tracing.endSpan();  // Manually end the span.
     *     }
     *   }
     * }
     * }</pre>
     *
     * @param tracerName name of {@link Tracer} that is obtained
     * @param spanName   name of {@link Span} that is used
     * @return the newly created {@code Span}.
     */
    Span startSpan(String tracerName, String spanName);

    /**
     * Same as {@link TracingProvider#startSpan(String, String)}, but you can start a {@link Span} that defines specific format for span name.
     * The final {@link Span} name consists of {@link Class#getSimpleName()}, and {@code spanSuffix}.
     *
     * <p><strong>Note:</strong> The preferred trace approach is to use {@code trace()} methods, such as
     * {@link TracingProvider#trace(Class, String, Consumer)}.</p> that controls the overall span lifecycle.
     *
     * <p>Example of usage:
     *
     * <pre>{@code tracing.startSpan(MyClass.name, "span")}</pre>
     * <p>
     * The result {@link Span} name will be "MyClass.span"
     *
     * @param tracerClass class that is used for getting tracer instance and its name used as prefix for {@link Span}
     * @param spanSuffix  suffix that is appended to the final {@link Span} name
     * @return the newly created {@code Span}.
     */
    default Span startSpan(Class<?> tracerClass, String spanSuffix) {
        return startSpan(tracerClass.getName(), tracerClass.getSimpleName() + "." + spanSuffix);
    }

    /**
     * Same as {@link TracingProvider#startSpan(String, String)}, but the {@link Span} is created from your own {@link SpanBuilder} instance.
     *
     * <p><strong>Note:</strong> The preferred trace approach is to use {@code trace()} methods, such as
     * {@link TracingProvider#trace(Class, String, Consumer)} that controls the overall span lifecycle.</p>
     *
     * <p>Example of usage:
     *
     * <pre>{@code tracing.startSpan(tracer.spanBuilder("mySpan"))}</pre>
     * <p>
     *
     * @param builder the custom {@link SpanBuilder}
     * @return the newly created {@code Span}.
     */
    Span startSpan(SpanBuilder builder);

    /**
     * Marks the end of the current {@link Span} execution.
     *
     * <p><strong>Note:</strong> The preferred trace approach is to use {@code trace()} methods, such as
     * {@link TracingProvider#trace(Class, String, Consumer)} that controls the overall span lifecycle.
     * In that case you should not use this method with {@code trace()} methods.</p>
     *
     */
    void endSpan();

    /**
     * Records information about the {@link Throwable} to the {@link Span}, and set {@link Span} status to {@link StatusCode#ERROR}
     *
     * @param exception the {@link Throwable} to record.
     */
    void error(Throwable exception);

    /**
     * Wrapper for code block {@code execution} which is traced and no need to manage the span lifecycle on our own.
     *
     * <p>Example of usage:
     *
     * <pre>{@code
     * class MyClass {
     *   private final TracingProvider tracing;
     *
     *   MyClass(KeycloakSession session) {
     *     tracing = session.getProvider(TracingProvider.class);
     *   }
     *
     *   void doWork() {
     *     tracing.trace("MyTracerName", "mySpanName", span -> {
     *         doSomeWork();
     *     });
     *   }
     * }
     * }</pre>
     *
     * @param tracerName name of {@link Tracer}
     * @param spanName   name of {@link Span}
     * @param execution  code that is executed and traced
     */
    void trace(String tracerName, String spanName, Consumer<Span> execution);

    /**
     * Wrapper for code block {@code execution} which is traced and no need to manage the span lifecycle on our own.
     * Same as {@link TracingProvider#trace(String, String, Consumer)}, but should be more usable.
     *
     * <p>Example of usage:
     *
     * <pre>{@code
     *   void doWork() {
     *     // creates span name 'MyClass.mySpanName'
     *     tracing.trace(MyClass.name, "mySpanName", span -> {
     *         doSomeWork();
     *     });
     *   }
     * }</pre>
     *
     * @param tracerClass class that is used for getting tracer instance and its name used as prefix for {@link Span}
     * @param spanSuffix  suffix that is appended to the final {@link Span} name
     * @param execution   code that is executed and traced
     */
    default void trace(Class<?> tracerClass, String spanSuffix, Consumer<Span> execution) {
        String className = prepareClassName(tracerClass);
        trace(className, className + "." + spanSuffix, execution);
    }

    private static String prepareClassName(Class<?> tracerClass) {
        String className = tracerClass.getSimpleName();
        if (className.isEmpty()) {
            className = tracerClass.getName();
            int end = className.lastIndexOf(".");
            if (end != -1) {
                className = className.substring(end + 1);
            }
        }
        return className;
    }

    /**
     * Wrapper for code block {@code execution} which is traced and no need to manage the span lifecycle on our own.
     *
     * <p>Example of usage:
     *
     * <pre>{@code
     * class MyClass {
     *   private final TracingProvider tracing;
     *
     *   MyClass(KeycloakSession session) {
     *     tracing = session.getProvider(TracingProvider.class);
     *   }
     *
     *   String doWork() {
     *     return tracing.trace("MyTracerName", "mySpanName", span -> {
     *         return returnSomeString();
     *     });
     *   }
     * }
     * }</pre>
     *
     * @param tracerName name of {@link Tracer}
     * @param spanName   name of {@link Span}
     * @param execution  code that is executed and traced that returns a value
     */
    <T> T trace(String tracerName, String spanName, Function<Span, T> execution);

    /**
     * Wrapper for code block {@code execution} that returns a value, is traced and no need to manage the span lifecycle on our own.
     * Same as {@link TracingProvider#trace(String, String, Function)}, but should be more usable.
     *
     * <p>Example of usage:
     *
     * <pre>{@code
     *   String doWork() {
     *     // creates span name 'MyClass.mySpanName'
     *     return tracing.trace(MyClass.name, "mySpanName", span -> {
     *         return returnSomeString();
     *     });
     *   }
     * }</pre>
     *
     * @param tracerClass class that is used for getting tracer instance and its name used as prefix for {@link Span}
     * @param spanSuffix  suffix that is appended to the final {@link Span} name
     * @param execution   code that is executed and traced
     */
    default <T> T trace(Class<?> tracerClass, String spanSuffix, Function<Span, T> execution) {
        String className = prepareClassName(tracerClass);
        return trace(className, className + "." + spanSuffix, execution);
    }

    /**
     * Gets or creates a named {@link Tracer} instance.
     *
     * @param name         name of the tracer
     * @param scopeVersion version of the instrumentation scope.
     */
    Tracer getTracer(String name, String scopeVersion);

    /**
     * Gets or creates a named {@link Tracer} instance.
     * Default scope version is Keycloak version (f.e. 26.0.5)
     *
     * @param name name of the tracer
     */
    default Tracer getTracer(String name) {
        return getTracer(name, Version.VERSION);
    }

    /**
     * Validates whether all started {@link Span} instances were ended.
     * <p>As part of the validation, some notification about not ended {@link Span}s should be shown.</p>
     *
     * @return true if all {@link Span}s ended
     */
    boolean validateAllSpansEnded();
}
