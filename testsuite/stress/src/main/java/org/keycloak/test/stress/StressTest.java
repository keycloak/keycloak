package org.keycloak.test.stress;

import jdk.nashorn.internal.codegen.CompilerConstants;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StressTest implements Callable<StressTest> {
    protected StressResult result;
    protected Callable<Boolean> test;
    protected int iterations;

    public StressTest(StressResult result, Callable<Boolean> test, int iterations) {
        this.result = result;
        this.test = test;
        this.iterations = iterations;
    }

    @Override
    public StressTest call() throws Exception {
        for (int i = 0; i < iterations; i++) {
            result.start();
            try {
                if (test.call()) {
                    result.success();
                }
            } catch (Throwable throwable) {
            }
            result.end();
        }
        return this;
    }
}
