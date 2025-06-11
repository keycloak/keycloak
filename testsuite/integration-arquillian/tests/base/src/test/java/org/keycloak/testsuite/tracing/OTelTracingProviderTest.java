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

package org.keycloak.testsuite.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.semconv.ExceptionAttributes;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;
import org.keycloak.tracing.NoopTracingProvider;
import org.keycloak.tracing.TracingProvider;

import java.io.Serializable;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@AuthServerContainerExclude(value = AuthServerContainerExclude.AuthServer.UNDERTOW)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OTelTracingProviderTest extends AbstractTestRealmKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

    private static boolean initialized;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void setContainer() {
        assertThat(suiteContext.getAuthServerInfo().isQuarkus(), is(true));

        if (!initialized) {
            startContainer();
            initialized = true;
        }
    }

    @Test
    // reset configuration and restart server as the last step of tests execution
    public void zzzzAfterClass() {
        try {
            var container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.resetConfiguration();
            container.restartServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getTestingClient().server(TEST_REALM_NAME).run(session -> {
            TracingProvider provider = session.getProvider(TracingProvider.class);
            assertThat(provider.getClass().getSimpleName(), is(NoopTracingProvider.class.getSimpleName()));
        });
    }

    void startContainer() {
        var containerQualifier = suiteContext.getAuthServerInfo().getQualifier();
        AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
        try {
            controller.stop(containerQualifier);
            container.setAdditionalBuildArgs(List.of("--tracing-enabled=true", "--log-level=org.keycloak.quarkus.runtime.tracing:debug"));
            controller.start(containerQualifier);
            reconnectAdminClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void parentSpan() {
        runOnServer(tracing -> {
            Span current = tracing.getCurrentSpan();
            assertThat(current, notNullValue());

            assertThat(current instanceof ReadableSpan, is(true));
            ReadableSpan readableSpan = (ReadableSpan) current;
            assertThat(readableSpan.getAttribute(AttributeKey.stringKey("code.function")), is("runOnServer"));
            assertThat(readableSpan.getAttribute(AttributeKey.stringKey("code.namespace")), is("org.keycloak.testsuite.rest.TestingResourceProvider"));
            assertThat(readableSpan.getName(), is("TestingResourceProvider.runOnServer"));
        });
    }

    @Test
    public void differentTracer() {
        runOnServer(tracing -> {
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
        runOnServer(tracing -> {
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
        runOnServer(tracing -> {
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

                assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_ESCAPED), is(true));
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
        runOnServer(tracing -> {
            tracing.trace(OpenTelemetry.class, "successful", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OpenTelemetry.successful"));
            });
        });
    }

    @Test
    public void traceSuccessfulValue() {
        runOnServer(tracing -> {
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
        runOnServer(tracing -> {
            try {
                tracing.trace(OTelTracingProviderTest.class, "errorSpan", span -> {
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.errorSpan"));

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
        runOnServer(tracing -> {
            tracing.trace(OTelTracingProviderTest.class, "test1", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test1"));

                tracing.trace(OTelTracingProviderTest.class, "test2", span2 -> {
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test2"));

                    // parent - span, child span2
                    assertThat(span.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));
                });
            });

            assertThat(tracing.validateAllSpansEnded(), is(true));
        });
    }

    @Test
    public void traceNestedSpansNotEnded() {
        runOnServer(tracing -> {
            tracing.trace(OTelTracingProviderTest.class, "test1", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test1"));

                var span2 = tracing.startSpan(OTelTracingProviderTest.class, "test2");
                try {
                    assertThat(span2, notNullValue());
                    assertThat(span2, is(tracing.getCurrentSpan()));
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test2"));
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
        runOnServer(tracing -> {
            var span1 = tracing.startSpan(OTelTracingProviderTest.class, "test1");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test1"));

            var span2 = tracing.startSpan(OTelTracingProviderTest.class, "test2");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test2"));
            // parent - span1, child span2
            assertThat(span1.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));

            var span3 = tracing.startSpan(OTelTracingProviderTest.class, "test3");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test3"));
            // parent - span2, child span3
            assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span3).getParentSpanContext().getSpanId()));

            var span4 = tracing.startSpan(OTelTracingProviderTest.class, "test4");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test4"));
            // parent - span3, child span4
            assertThat(span3.getSpanContext().getSpanId(), is(((ReadableSpan) span4).getParentSpanContext().getSpanId()));
            tracing.endSpan();

            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test3"));

            var span5 = tracing.startSpan(OTelTracingProviderTest.class, "test5");
            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test5"));
            // parent - span3, child span5
            assertThat(span1.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));

            assertThat(tracing.validateAllSpansEnded(), is(false));
        });
    }

    @Test
    public void traceMultipleAlwaysEnded() {
        runOnServer(tracing -> {
            // Level 1
            tracing.trace(OTelTracingProviderTest.class, "test1", span -> {
                assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test1"));

                // Level 2
                tracing.trace(OTelTracingProviderTest.class, "test2", span2 -> {
                    assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test2"));
                    // parent - span, child span2
                    assertThat(span.getSpanContext().getSpanId(), is(((ReadableSpan) span2).getParentSpanContext().getSpanId()));

                    // Level 3
                    tracing.trace(OTelTracingProviderTest.class, "test3", span3 -> {
                        assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test3"));
                        // parent - span2, child span3
                        assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span3).getParentSpanContext().getSpanId()));
                    });

                    tracing.trace(OTelTracingProviderTest.class, "test4", span4 -> {
                        assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test4"));
                        // parent - span2, child span4
                        assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span4).getParentSpanContext().getSpanId()));
                    });

                    tracing.trace(OTelTracingProviderTest.class, "test5", span5 -> {
                        assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test5"));
                        // parent - span2, child span5
                        assertThat(span2.getSpanContext().getSpanId(), is(((ReadableSpan) span5).getParentSpanContext().getSpanId()));

                        // Level 4
                        tracing.trace(OTelTracingProviderTest.class, "test6", span6 -> {
                            assertThat(((ReadableSpan) tracing.getCurrentSpan()).toSpanData().getName(), is("OTelTracingProviderTest.test6"));
                            // parent - span5, child span6
                            assertThat(span5.getSpanContext().getSpanId(), is(((ReadableSpan) span6).getParentSpanContext().getSpanId()));
                        });
                    });
                });
            });
            assertThat(tracing.validateAllSpansEnded(), is(true));
        });
    }

    void runOnServer(TracingConsumer tracing) {
        getTestingClient().server(TEST_REALM_NAME).run(session -> {
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
}
