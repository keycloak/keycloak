package org.keycloak.test.stress;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes all test threads until completion.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StressExecutor {
    protected List<StressTest> tests = new LinkedList<>();
    protected List<StressResult> results = new LinkedList<>();

    public void addTest(Class<? extends Test> test, int threads, int iterations) {
        StressResult result = new StressResult(test.getName());
        results.add(result);
        for (int i = 0; i < threads; i++) {
            try {
                Test t = test.newInstance();
                t.init();
                StressTest stress = new StressTest(result, t, iterations);
                tests.add(stress);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addTest(Test test, StressResult result, int iterations) {
        tests.add(new StressTest(result, test, iterations));
    }

    public void addTest(Test test, int iterations) {
        StressResult result = new StressResult(test.getClass().getName());
        tests.add(new StressTest(result, test, iterations));
    }

    public long execute() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(tests.size());
        Collections.shuffle(tests);
        long start = System.currentTimeMillis();
        for (StressTest test : tests) {
            executor.submit(test);
        }
        executor.shutdown();
        boolean done = executor.awaitTermination(100, TimeUnit.HOURS);
        long end = System.currentTimeMillis() - start;
        return end;

    }
}
