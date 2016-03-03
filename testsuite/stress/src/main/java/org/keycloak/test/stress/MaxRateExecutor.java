package org.keycloak.test.stress;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes a test N number of times.  This is done multiple times over an ever expanding amount of threads to determine
 * when the computer is saturated and you can't eek out any more concurrent requests.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MaxRateExecutor {

    public static class RateResult {
        StressResult result;
        int threads;
        long time;

        public RateResult(StressResult result, int threads, long time) {
            this.result = result;
            this.threads = threads;
            this.time = time;
        }

        public StressResult getResult() {
            return result;
        }

        public int getThreads() {
            return threads;
        }

        public long getTime() {
            return time;
        }
    }

    List<RateResult> allResults = new LinkedList<>();
    RateResult fastest = null;
    RateResult last = null;



    public void best(TestFactory factory, int jobs) {
        fastest = last = null;
        int threads = 2;
        do {
            fastest = last;
            try {
                last = execute(factory, threads, jobs);
                allResults.add(last);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            threads++;
        } while (fastest == null || fastest.time > last.time);
    }

    public RateResult getFastest() {
        return fastest;
    }

    public RateResult getLast() {
        return last;
    }

    public RateResult execute(TestFactory factory, int threads, int jobs) throws InterruptedException, ExecutionException {
        List<StressTest> tests = new LinkedList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ExecutorCompletionService<StressTest> completionService = new ExecutorCompletionService<>(executor);
        StressResult result = new StressResult("num threads:" + threads);
        addTest(factory, result, tests, threads + 5);
        long start = System.currentTimeMillis();
        for (StressTest stressTest : tests) {
            completionService.submit(stressTest);
        }
        for (int i = 0; i < jobs; i++) {
            Future<StressTest> future = completionService.take();
            StressTest stressTest = future.get();
            if (i < jobs - threads - 5) completionService.submit(stressTest);
        }
        long end = System.currentTimeMillis() - start;
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        RateResult rate = new RateResult(result, threads, end);
        return rate;
    }

    private void addTest(TestFactory factory, StressResult result, List<StressTest> tests, int num) {
        int add = num - tests.size();
        for (int i = 0; i < add; i++) {
            Test test = factory.create();
            test.init();
            StressTest stress = new StressTest(result, test, 1);
            tests.add(stress);
        }
    }

    public void printResults() {
        System.out.println("*******************");
        System.out.println("*   Best Result   *");
        System.out.println("*******************");
        printResult(fastest);
    }


    public void printResult(RateResult result) {
        System.out.println("Threads: " + result.getThreads());
        System.out.println("Total Time: " + result.getTime());
        System.out.println("Rate: " + ((double)result.getResult().getIterations()) / ((double)result.getTime()));
        System.out.println("Successes: " + result.getResult().getSuccess());
        System.out.println("Iterations: " + result.getResult().getIterations());
        System.out.println("Average time per iteration: " + result.getResult().getAverageTime());

    }

    public void printSummary() {

        for (RateResult result : allResults) {
            System.out.println("*******************");
            printSummary(result);
        }
    }
    public void printSummary(RateResult result) {
        System.out.println("Threads: " + result.getThreads());
        System.out.println("Total Time: " + result.getTime());
    }
}
