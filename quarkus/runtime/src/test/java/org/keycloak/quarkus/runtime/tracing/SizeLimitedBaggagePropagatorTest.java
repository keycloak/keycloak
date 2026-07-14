package org.keycloak.quarkus.runtime.tracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that {@link SizeLimitedBaggagePropagator} enforces W3C Baggage limits,
 * preventing CVE-2026-45292 regardless of the underlying OTel SDK version.
 */
public class SizeLimitedBaggagePropagatorTest {

    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }

        @Override
        public Iterator<String> getAll(Map<String, String> carrier, String key) {
            String value = carrier.get(key);
            return value != null
                    ? Collections.singletonList(value).iterator()
                    : Collections.emptyIterator();
        }
    };

    private final TextMapPropagator propagator =
            new SizeLimitedBaggagePropagator(W3CBaggagePropagator.getInstance());

    @Test
    public void testExcessiveEntryCountIsBlocked() {
        int attackEntryCount = 500;
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < attackEntryCount; i++) {
            if (i > 0) header.append(",");
            header.append("key").append(i).append("=value").append(i);
        }

        Map<String, String> carrier = new HashMap<>();
        carrier.put("baggage", header.toString());

        Context ctx = propagator.extract(Context.root(), carrier, GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Oversized baggage (500 entries) must be dropped entirely", 0, baggage.size());
    }

    @Test
    public void testOversizedBaggageHeaderIsBlocked() {
        String longValue = "x".repeat(SizeLimitedBaggagePropagator.MAX_BAGGAGE_BYTES);
        String header = "oversized-key=" + longValue;

        Map<String, String> carrier = new HashMap<>();
        carrier.put("baggage", header);

        Context ctx = propagator.extract(Context.root(), carrier, GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Oversized baggage (>8192 bytes) must be dropped entirely", 0, baggage.size());
    }

    @Test
    public void testNormalBaggagePassesThrough() {
        Map<String, String> carrier = new HashMap<>();
        carrier.put("baggage", "tenant=acme,request-id=abc-123,feature-flag=dark-mode");

        Context ctx = propagator.extract(Context.root(), carrier, GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Normal baggage (3 entries) must be extracted", 3, baggage.size());
        assertEquals("acme", baggage.getEntryValue("tenant"));
        assertEquals("abc-123", baggage.getEntryValue("request-id"));
        assertEquals("dark-mode", baggage.getEntryValue("feature-flag"));
    }

    @Test
    public void testBaggageAtExactEntryLimitPassesThrough() {
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < SizeLimitedBaggagePropagator.MAX_BAGGAGE_ENTRIES; i++) {
            if (i > 0) header.append(",");
            header.append("k").append(i).append("=v").append(i);
        }

        Map<String, String> carrier = new HashMap<>();
        carrier.put("baggage", header.toString());

        Context ctx = propagator.extract(Context.root(), carrier, GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Baggage at exactly the entry limit must be extracted",
                SizeLimitedBaggagePropagator.MAX_BAGGAGE_ENTRIES, baggage.size());
    }

    @Test
    public void testBaggageOneOverEntryLimitIsBlocked() {
        int overLimit = SizeLimitedBaggagePropagator.MAX_BAGGAGE_ENTRIES + 1;
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < overLimit; i++) {
            if (i > 0) header.append(",");
            header.append("k").append(i).append("=v").append(i);
        }

        Map<String, String> carrier = new HashMap<>();
        carrier.put("baggage", header.toString());

        Context ctx = propagator.extract(Context.root(), carrier, GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Baggage one over the entry limit must be dropped", 0, baggage.size());
    }

    @Test
    public void testBaggageAtExactByteLimitPassesThrough() {
        String key = "k";
        String value = "x".repeat(SizeLimitedBaggagePropagator.MAX_BAGGAGE_BYTES - key.length() - 1);
        String header = key + "=" + value;
        assertEquals(SizeLimitedBaggagePropagator.MAX_BAGGAGE_BYTES, header.length());

        Map<String, String> carrier = new HashMap<>();
        carrier.put("baggage", header);

        Context ctx = propagator.extract(Context.root(), carrier, GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Baggage at exactly the byte limit must be extracted", 1, baggage.size());
    }

    @Test
    public void testMultipleHeadersCumulativeEntryCountIsBlocked() {
        MultiValueCarrier carrier = new MultiValueCarrier();
        for (int i = 0; i < 5; i++) {
            StringBuilder header = new StringBuilder();
            for (int j = 0; j < 20; j++) {
                if (j > 0) header.append(",");
                header.append("k").append(i * 20 + j).append("=v").append(i * 20 + j);
            }
            carrier.add("baggage", header.toString());
        }

        Context ctx = propagator.extract(Context.root(), carrier, MULTI_GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Multiple headers with 100 cumulative entries must be dropped", 0, baggage.size());
    }

    @Test
    public void testMultipleHeadersCumulativeBytesIsBlocked() {
        MultiValueCarrier carrier = new MultiValueCarrier();
        String chunk = "k=" + "x".repeat(5000);
        carrier.add("baggage", chunk);
        carrier.add("baggage", chunk);

        Context ctx = propagator.extract(Context.root(), carrier, MULTI_GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("Multiple headers exceeding cumulative byte limit must be dropped", 0, baggage.size());
    }

    @Test
    public void testNoBaggageHeaderPassesThrough() {
        Map<String, String> carrier = new HashMap<>();

        Context ctx = propagator.extract(Context.root(), carrier, GETTER);
        Baggage baggage = Baggage.fromContext(ctx);

        assertEquals("No baggage header must result in empty baggage", 0, baggage.size());
    }

    private static class MultiValueCarrier {
        private final Map<String, List<String>> headers = new HashMap<>();

        void add(String key, String value) {
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    private static final TextMapGetter<MultiValueCarrier> MULTI_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(MultiValueCarrier carrier) {
            return carrier.headers.keySet();
        }

        @Override
        public String get(MultiValueCarrier carrier, String key) {
            List<String> values = carrier.headers.get(key);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }

        @Override
        public Iterator<String> getAll(MultiValueCarrier carrier, String key) {
            List<String> values = carrier.headers.get(key);
            return values != null ? values.iterator() : Collections.emptyIterator();
        }
    };
}
