package org.keycloak.quarkus.runtime.tracing;

import java.util.Collection;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.jboss.logging.Logger;

/**
 * Wraps a {@link TextMapPropagator} (typically {@code W3CBaggagePropagator}) and enforces
 * W3C Baggage specification size limits before delegating extraction.
 *
 * <p>CVE-2026-45292: OTel SDK &le; 1.61.0 performs unbounded memory allocation when parsing
 * oversized {@code baggage} headers. This wrapper short-circuits extraction when the header
 * exceeds the spec limits, preventing the vulnerable code path from executing.
 *
 * @see <a href="https://github.com/advisories/GHSA-rcgg-9c38-7xpx">GHSA-rcgg-9c38-7xpx</a>
 * @see <a href="https://www.w3.org/TR/baggage/#limits">W3C Baggage - Limits</a>
 */
public class SizeLimitedBaggagePropagator implements TextMapPropagator {

    private static final Logger log = Logger.getLogger(SizeLimitedBaggagePropagator.class);

    static final int MAX_BAGGAGE_BYTES = 8192;
    static final int MAX_BAGGAGE_ENTRIES = 64;
    private static final String BAGGAGE_HEADER = "baggage";

    private final TextMapPropagator delegate;

    public SizeLimitedBaggagePropagator(TextMapPropagator delegate) {
        this.delegate = delegate;
    }

    @Override
    public Collection<String> fields() {
        return delegate.fields();
    }

    @Override
    public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
        delegate.inject(context, carrier, setter);
    }

    @Override
    public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
        if (context == null) {
            context = Context.root();
        }
        if (carrier == null || getter == null) {
            return context;
        }

        // Uses get() instead of getAll() (added in OTel 1.45.0) — equivalent protection since
        // OTel 1.44.x propagator itself only reads a single header
        String header = getter.get(carrier, BAGGAGE_HEADER);
        if (header == null || header.isEmpty()) {
            return delegate.extract(context, carrier, getter);
        }

        if (header.length() > MAX_BAGGAGE_BYTES) {
            log.debugf("Dropping oversized baggage header (%d bytes, max %d)", header.length(), MAX_BAGGAGE_BYTES);
            return context;
        }
        if (countEntries(header) > MAX_BAGGAGE_ENTRIES) {
            log.debugf("Dropping baggage header with too many entries (%d, max %d)", countEntries(header), MAX_BAGGAGE_ENTRIES);
            return context;
        }

        return delegate.extract(context, carrier, getter);
    }

    private static int countEntries(String baggage) {
        int count = 1;
        for (int i = 0, len = baggage.length(); i < len; i++) {
            if (baggage.charAt(i) == ',') {
                count++;
            }
        }
        return count;
    }
}
