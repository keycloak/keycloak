/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.protocol;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.jgroups.header.TracerHeader;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.jgroups.Message;
import org.jgroups.Version;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.stack.Protocol;
import org.jgroups.util.MessageBatch;

/**
 * Provides Open Telemetry (https://opentelemetry.io/) tracing for JGroups. It should be placed just above the
 * transport.<br/>
 * When a message is sent, a {@link TracerHeader} is added with the (optional) parent span.
 * When received a new span is started (as a child span, if the parent span in the header is non-null), and ended when
 * the the thread returns.
 *
 * @author Bela Ban
 * @since 1.0.0
 */
@MBean(description = "Records OpenTelemetry traces of sent and received messages")
public class OPEN_TELEMETRY extends Protocol {
    public static final short OPEN_TELEMETRY_ID = 1026;
    protected OpenTelemetry otel;
    protected Tracer tracer;

    @Property(description = "When active, traces are recorded, otherwise not")
    protected boolean active = true;

    public boolean active() {
        return active;
    }

    public OPEN_TELEMETRY active(boolean f) {
        active = activate(f);
        return this;
    }

    public void start() throws Exception {
        super.start();
        activate(active);
    }

    public Object down(Message msg) {
        if (!active || !Span.current().getSpanContext().isValid())
            return down_prot.down(msg);

        SpanBuilder spanBuilder = tracer.spanBuilder("JGroups.sendSingleMessage");
        if (Span.current().isRecording()) {
            if (msg.getDest() != null) {
                spanBuilder.setAttribute("kc.jgroups.dest", msg.getDest().toString());
            }
            if (msg.getSrc() != null) {
                spanBuilder.setAttribute("kc.jgroups.src", msg.getSrc().toString());
            }
        }
        Span span = spanBuilder.startSpan();
        try (var ignored = span.makeCurrent()) {
            TracerHeader hdr = new TracerHeader();
            populateHeader(hdr); // will populate if a span exists (created by the caller)
            msg.putHeader(OPEN_TELEMETRY_ID, hdr);
            return down_prot.down(msg);
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, String.format("failed delivering single message to %s", msg.dest()));
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }


    public Object up(Message msg) {
        if (!active)
            return up_prot.up(msg);

        TracerHeader hdr = msg.getHeader(OPEN_TELEMETRY_ID);
        if (hdr != null) {
            Context extractedContext = otel.getPropagators().getTextMapPropagator()
                    .extract(Context.current(), hdr, TEXT_MAP_GETTER);

            Span span = tracer.spanBuilder("JGroups.deliverSingleMessage")
                    .setSpanKind(SpanKind.SERVER)
                    .setParent(extractedContext).startSpan();

            try (Scope ignored = span.makeCurrent()) {
                span.setAttribute("from", msg.src().toString());
                return up_prot.up(msg);
            } catch (Throwable t) {
                span.setStatus(StatusCode.ERROR, String.format("failed delivering single message from %s", msg.src()));
                span.recordException(t);
                throw t;
            } finally {
                span.end();
            }
        } else {
            return up_prot.up(msg);
        }
    }

    public void up(MessageBatch batch) {
        if (!active) {
            if (!batch.isEmpty())
                up_prot.up(batch);
            return;
        }
        List<Span> spans = new ArrayList<>(batch.size());
        int index = 0, batch_size = batch.size();
        for (Message msg : batch) {
            index++;
            TracerHeader hdr = msg.getHeader(OPEN_TELEMETRY_ID);
            if (hdr != null) {
                Context extractedContext = otel.getPropagators().getTextMapPropagator()
                        .extract(Context.current(), hdr, TEXT_MAP_GETTER);

                Span span = tracer.spanBuilder("deliver-batched-msg")
                        .setSpanKind(SpanKind.SERVER)
                        .setParent(extractedContext).startSpan();
                span.setAttribute("batch-msg", String.format("%d/%d", index, batch_size));
                spans.add(span);
            }
        }
        try {
            if (!batch.isEmpty())
                up_prot.up(batch);
        } catch (Throwable t) {
            spans.forEach(s -> {
                s.setStatus(StatusCode.ERROR, String.format("failed delivering batched message from %s", batch.sender()))
                        .recordException(t);
            });
            throw t;
        } finally {
            spans.forEach(Span::end);
        }
    }

    protected static void populateHeader(TracerHeader hdr) {
        // Inject the request with the *current* Context, which contains our current Span.
        W3CTraceContextPropagator.getInstance().inject(Context.current(), hdr, (carrier, key, val) -> hdr.put(key, val));
    }

    protected static final TextMapGetter<TracerHeader> TEXT_MAP_GETTER =
            new TextMapGetter<>() {
                @Override
                public String get(TracerHeader carrier, String key) {
                    return carrier.get(key);
                }

                @Override
                public Iterable<String> keys(TracerHeader carrier) {
                    return carrier.keys();
                }
            };

    protected boolean activate(boolean flag) {
        if (flag && otel == null)
            otel = GlobalOpenTelemetry.get();
        if (flag && tracer == null)
            tracer = otel.getTracer("org.jgroups.trace", Version.printVersion());
        return flag;
    }

}
