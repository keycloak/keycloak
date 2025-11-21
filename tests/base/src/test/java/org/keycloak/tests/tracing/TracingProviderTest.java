package org.keycloak.tests.tracing;

import java.io.Serializable;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tracing.TracingProvider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.ExceptionAttributes;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest(config = TracingProviderTest.ServerConfigWithTracing.class)
public class TracingProviderTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void parentSpan() {
        runOnServerTracing(tracing -> {
            Span current = tracing.getCurrentSpan();
            assertThat(current, notNullValue());

            assertThat(current instanceof ReadableSpan, is(true));
            ReadableSpan readableSpan = (ReadableSpan) current;
            assertThat(readableSpan.getAttribute(AttributeKey.stringKey("code.function")), is("runOnServer"));
            assertThat(readableSpan.getAttribute(AttributeKey.stringKey("code.namespace")), is("org.keycloak.testframework.remote.providers.runonserver.RunOnServerRealmResourceProvider"));
            assertThat(readableSpan.getName(), is("RunOnServerRealmResourceProvider.runOnServer"));
        });
    }

    @Test
    public void differentTracer() {
        runOnServerTracing(tracing -> {
            var tracer1 = tracing.getTracer("tracer1");
            var tracer2 = tracing.getTracer("tracer2");
            var tracer3 = tracing.getTracer("tracer1");

            assertThat(tracer1, notNullValue());
            assertThat(tracer2, notNullValue());
            assertThat(tracer3, notNullValue());

            assertThat(tracer1.equals(tracer2), is(false));
            assertThat(tracer2.equals(tracer3), is(false));
            assertThat(tracer3.equals(tracer1), is(true));

            var tracerOldVersion = tracing.getTracer("tracer1", "25.0.0");
            var tracerNewVersion = tracing.getTracer("tracer1", "26.0.0");

            assertThat(tracerOldVersion, notNullValue());
            assertThat(tracerNewVersion, notNullValue());

            assertThat(tracerOldVersion.equals(tracerNewVersion), is(false));
        });
    }

    @Test
    public void spanInfo() {
        runOnServerTracing(tracing -> {
            var span = tracing.startSpan("MyTracer", "sameSpan");
            try {
                var current = tracing.getCurrentSpan();
                assertThat(current, notNullValue());
                assertThat(current.equals(span), is(true));

                var context = current.getSpanContext();
                assertThat(context, is(span.getSpanContext()));
                assertThat(context.getTraceFlags(), is(TraceFlags.getSampled()));
                assertThat(current.isRecording(), is(true));

                assertThat(current instanceof ReadableSpan, is(true));
                var readableSpan = (ReadableSpan) current;
                assertThat(readableSpan.getName(), is("sameSpan"));
            } finally {
                tracing.endSpan();
            }

            assertThat(span.isRecording(), is(false));

            var current = tracing.getCurrentSpan();
            assertThat(current, is(not(span)));
        });
    }

    @Test
    public void errorInSpan() {
        runOnServerTracing(tracing -> {
            var span = tracing.startSpan("MyTracer", "something");
            try {
                try {
                    var current = tracing.getCurrentSpan();
                    assertThat(current, notNullValue());
                    assertThat(current.equals(span), is(true));
                    throw new RuntimeException("something bad happened");
                } catch (Exception e) {
                    tracing.error(e);
                } finally {
                    // not ended span here
                }

                var current = tracing.getCurrentSpan();
                assertThat(current, is(span));

                assertThat(current instanceof ReadableSpan, is(true));
                var spanData = ((ReadableSpan) current).toSpanData();

                assertThat(spanData.getStatus(), notNullValue());
                assertThat(spanData.getStatus(), is(StatusData.create(StatusCode.ERROR, "something bad happened")));

                assertThat(spanData.getName(), is("something"));
                assertThat(spanData.getTotalRecordedEvents(), is(1));

                var eventData = spanData.getEvents().get(0);
                assertThat(eventData instanceof ExceptionEventData, is(true));

                var exceptionData = (ExceptionEventData) eventData;
                var exceptionAttributes = exceptionData.getAttributes();
                assertThat(exceptionAttributes, notNullValue());
                assertThat(exceptionAttributes.get(AttributeKey.booleanKey("exception.escaped")), is(true));
                assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_MESSAGE), is("something bad happened"));
                assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE), not(emptyOrNullString()));
                assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_TYPE), is(RuntimeException.class.getCanonicalName()));
            } finally {
                tracing.endSpan();
            }
        });
    }

    @Test
    public void traceSuccessful() {
        runOnServerTracing(tracing -> {
            tracing.trace(OpenTelemetry.class, "successful", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OpenTelemetry.successful"));
            });
        });
    }

    @Test
    public void traceSuccessfulValue() {
        runOnServerTracing(tracing -> {
            var spanName = tracing.trace(OpenTelemetry.class, "successful", (span) -> {
                var finalName = "OpenTelemetry.successful";
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is(finalName));
                return finalName;
            });

            assertThat(spanName, notNullValue());
            assertThat(spanName, is("OpenTelemetry.successful"));
        });
    }

    @Test
    public void traceWithError() {
        runOnServerTracing(tracing -> {
            try {
                tracing.trace(TracingProviderTest.class, "errorSpan", span -> {
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.errorSpan"));

                    if (true) {
                        throw new IllegalStateException("some invalid error");
                    }
                });
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(), is("some invalid error"));
                return;
            }
            throw new AssertionError("The IllegalStateException was not propagated");
        });
    }

    @Test
    public void traceNestedSpans() {
        runOnServerTracing(tracing -> {
            tracing.trace(TracingProviderTest.class, "test1", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test1"));

                tracing.trace(TracingProviderTest.class, "test2", span2 -> {
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test2"));

                    // parent - span, child span2
                    assertThat(span.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));
                });
            });

            assertThat(tracing.validateAllSpansEnded(), is(true));
        });
    }

    @Test
    public void traceNestedSpansNotEnded() {
        runOnServerTracing(tracing -> {
            tracing.trace(TracingProviderTest.class, "test1", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test1"));

                var span2 = tracing.startSpan(TracingProviderTest.class, "test2");
                try {
                    assertThat(span2, notNullValue());
                    assertThat(span2, is(tracing.getCurrentSpan()));
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test2"));
                    // parent - span, child span2
                    assertThat(span.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));
                } finally {
                    // not ended
                }
            });

            assertThat(tracing.validateAllSpansEnded(), is(false));
        });
    }

    @Test
    public void multipleNotEnded() {
        runOnServerTracing(tracing -> {
            var span1 = tracing.startSpan(TracingProviderTest.class, "test1");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test1"));

            var span2 = tracing.startSpan(TracingProviderTest.class, "test2");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test2"));
            // parent - span1, child span2
            assertThat(span1.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));

            var span3 = tracing.startSpan(TracingProviderTest.class, "test3");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test3"));
            // parent - span2, child span3
            assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span3).getParentSpanContext().getSpanId()));

            var span4 = tracing.startSpan(TracingProviderTest.class, "test4");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test4"));
            // parent - span3, child span4
            assertThat(span3.getSpanContext().getSpanId(), is(((ReadableSpan) span4).getParentSpanContext().getSpanId()));
            tracing.endSpan();

            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test3"));

            var span5 = tracing.startSpan(TracingProviderTest.class, "test5");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test5"));
            // parent - span3, child span5
            assertThat(span1.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));

            assertThat(tracing.validateAllSpansEnded(), is(false));
        });
    }

    @Test
    public void traceMultipleAlwaysEnded() {
        runOnServerTracing(tracing -> {
            // Level 1
            tracing.trace(TracingProviderTest.class, "test1", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test1"));

                // Level 2
                tracing.trace(TracingProviderTest.class, "test2", span2 -> {
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test2"));
                    // parent - span, child span2
                    assertThat(span.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));

                    // Level 3
                    tracing.trace(TracingProviderTest.class, "test3", span3 -> {
                        assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test3"));
                        // parent - span2, child span3
                        assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span3).getParentSpanContext().getSpanId()));
                    });

                    tracing.trace(TracingProviderTest.class, "test4", span4 -> {
                        assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test4"));
                        // parent - span2, child span4
                        assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span4).getParentSpanContext().getSpanId()));
                    });

                    tracing.trace(TracingProviderTest.class, "test5", span5 -> {
                        assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test5"));
                        // parent - span2, child span5
                        assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span5).getParentSpanContext().getSpanId()));

                        // Level 4
                        tracing.trace(TracingProviderTest.class, "test6", span6 -> {
                            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("TracingProviderTest.test6"));
                            // parent - span5, child span6
                            assertThat(span5.getSpanContext().getSpanId(), is(((ReadableSpan) span6).getParentSpanContext().getSpanId()));
                        });
                    });
                });
            });
            assertThat(tracing.validateAllSpansEnded(), is(true));
        });
    }

    void runOnServerTracing(TracingConsumer tracing) {
        runOnServer.run(session -> {
            TracingProvider provider = session.getProvider(TracingProvider.class);
            assertThat(provider.getClass().getSimpleName(), is("OTelTracingProvider"));
            tracing.accept(provider);

            // cleanup not ended spans
            while (!provider.validateAllSpansEnded()) {
                provider.endSpan();
            }
        });
    }

    @FunctionalInterface
    interface TracingConsumer extends Serializable {
        void accept(TracingProvider tracing);
    }

    public static class ServerConfigWithTracing implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .option("tracing-enabled", "true")
                    .option("log-level", "INFO,org.keycloak.quarkus.runtime.tracing:debug");
        }
    }
}
