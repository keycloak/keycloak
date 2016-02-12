package org.keycloak.test.stress;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StressResult {
    ThreadLocal<Long> start = new ThreadLocal<>();
    AtomicLong iterations = new AtomicLong();
    AtomicLong totalTime = new AtomicLong();
    String name;
    AtomicInteger success = new AtomicInteger();

    public StressResult(String name) {
        this.name = name;
    }

    public void start() {
        start.set(System.currentTimeMillis());
    }

    public void success() {
        success.incrementAndGet();
    }

    public void end() {
        long end = System.currentTimeMillis() - start.get();
        totalTime.addAndGet(end);
        iterations.incrementAndGet();
    }

    public int getSuccess() {
        return success.get();
    }

    public String getName() {
        return name;
    }

    public long getTotalTime() {
        return totalTime.longValue();
    }
    public long getIterations() {
        return iterations.get();
    }

    public double getAverageTime() {
        return (double)(double)totalTime.get() / (double)iterations.get();
    }
    public double getRate() {
        return (double)(double)iterations.get() / (double)totalTime.get();
    }

    public void clear() {
        iterations.set(0);
        totalTime.set(0);
        success.set(0);
    }
}
