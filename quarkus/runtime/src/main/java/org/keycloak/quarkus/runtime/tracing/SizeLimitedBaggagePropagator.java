package org.keycloak.quarkus.runtime.tracing;

import java.util.Collection;
import java.util.Iterator;

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

        Iterator<String> headers = getter.getAll(carrier, BAGGAGE_HEADER);
        if (headers == null || !headers.hasNext()) {
            return delegate.extract(context, carrier, getter);
        }

        int totalBytes = 0;
        int totalEntries = 0;
        while (headers.hasNext()) {
            String header = headers.next();
            if (header == null || header.isEmpty()) {
                continue;
            }
            totalBytes += header.length();
            if (totalBytes > MAX_BAGGAGE_BYTES) {
                log.debugf("Dropping oversized baggage headers (%d bytes cumulative, max %d)", totalBytes, MAX_BAGGAGE_BYTES);
                return context;
            }
            totalEntries += countEntries(header);
            if (totalEntries > MAX_BAGGAGE_ENTRIES) {
                log.debugf("Dropping baggage headers with too many entries (%d cumulative, max %d)", totalEntries, MAX_BAGGAGE_ENTRIES);
                return context;
            }
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
