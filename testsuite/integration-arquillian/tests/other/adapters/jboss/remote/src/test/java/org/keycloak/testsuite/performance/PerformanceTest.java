package org.keycloak.testsuite.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.performance.metrics.impl.Results;
import org.keycloak.testsuite.performance.metrics.impl.ResultsWithThroughput;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * PerformanceTest.
 *
 * @author tkyjovsk
 */
public abstract class PerformanceTest extends AbstractExampleAdapterTest {

    private final Logger LOG = Logger.getLogger(PerformanceTest.class);

    public static final Integer WARMUP_LOAD = Integer.parseInt(System.getProperty("warmup.load", "5"));
    public static final Integer WARMUP_DURATION = Integer.parseInt(System.getProperty("warmup.duration", "30"));

    public static final Integer INITIAL_LOAD = Integer.parseInt(System.getProperty("initial.load", "10")); // load for the first iteration
    public static final Integer LOAD_INCREASE = Integer.parseInt(System.getProperty("load.increase", "10")); // how many threads to add before each iteration
    public static final Integer LOAD_INCREASE_RATE = Integer.parseInt(System.getProperty("load.increase.rate", "2")); // how fast to add the new threads per second

    public static final Integer MEASUREMENT_DURATION = Integer.parseInt(System.getProperty("measurement.duration", "20")); // duration of one measurement iteration

    public static final Integer MAX_ITERATIONS = Integer.parseInt(System.getProperty("max.iterations", "10"));
    public static final Integer MAX_THREADS = Integer.parseInt(System.getProperty("max.threads", "1000"));

    public static final Integer SLEEP_BETWEEN_REPEATS = Integer.parseInt(System.getProperty("sleep.between.repeats", "0"));

    private final double AVERAGE_TIMEOUT_PERCENTAGE_LIMIT = Double.parseDouble(System.getProperty("average.timeout.percentage.limit", "0.01"));

    private int currentLoad;

    private ExecutorService executorService;

    protected PerformanceTestMetrics metrics = new PerformanceTestMetrics();
    protected PerformanceTestMetrics timeouts = new PerformanceTestMetrics();

    protected List<ResultsWithThroughput> resultsList = new ArrayList<>();
    protected List<ResultsWithThroughput> timeoutResultsList = new ArrayList<>();

    @Before
    public void before() {
        if (WARMUP_LOAD > INITIAL_LOAD) {
            throw new IllegalArgumentException("'warmup.load' cannot be larger than 'initial.load'");
        }

        executorService = Executors.newFixedThreadPool(MAX_THREADS);
        currentLoad = 0;

        metrics.clear();
    }

    @After
    public void after() throws IOException, InterruptedException {
        executorService.shutdown();
        LOG.info("Waiting for threadpool termination.");
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void test() {

        increaseLoadBy(WARMUP_LOAD); // increase to warmup load
        warmup();

        for (int i = 0; i < MAX_ITERATIONS; i++) {

            int loadIncrease = (i == 0)
                    ? INITIAL_LOAD - WARMUP_LOAD // increase from warmup to initial load
                    : LOAD_INCREASE; // increase load between iterations

            increaseLoadBy(loadIncrease);
            measurePerformance();

            if (!isThereEnoughThreadsForNextIteration(LOAD_INCREASE)) {
                LOG.warn("Threadpool capacity reached. Stopping the test.");
                break;
            }
            if (!isLatestResultsWithinLimits()) {
                LOG.warn("The latest measurement surpassed expected limit. Stopping the test.");
                break;
            }
        }

    }

    private void warmup() {
        LOG.info("Warming up for " + WARMUP_DURATION + " s");
        pauseWithErrorChecking(WARMUP_DURATION * 1000);
    }

    private boolean isThereEnoughThreadsForNextIteration(int loadIncrease) {
        return currentLoad + loadIncrease <= MAX_THREADS;
    }

    private void increaseLoadBy(int loadIncrease) {
        if (loadIncrease < 0) {
            throw new IllegalArgumentException("Cannot increase load by a negative number (" + loadIncrease + ").");
        }
        if (!isThereEnoughThreadsForNextIteration(loadIncrease)) {
            throw new IllegalArgumentException("Cannot increase load beyond threadpool capacity (" + MAX_THREADS + ").");
        }
        if (loadIncrease > 0) {
            LOG.info(String.format("Increasing load from %s to %s.", currentLoad, currentLoad + loadIncrease));
            for (int t = 0; t < loadIncrease; t++) {
                executorService.submit(newRunnable());
                currentLoad++;
                pauseWithErrorChecking(1000 / LOAD_INCREASE_RATE);
            }
        }
    }

    private void measurePerformance() {
        LOG.info("Measuring performance");
        LOG.info("Iteration: " + (resultsList.size() + 1));
        LOG.info("Duration: " + MEASUREMENT_DURATION + " s");
        LOG.info("Load: " + currentLoad);

        metrics.reset();
        pauseWithErrorChecking(MEASUREMENT_DURATION * 1000);
        resultsList.add(metrics.computeMetrics());
        timeoutResultsList.add(timeouts.computeMetrics());

        getLatestResults().logResults(); // to file
        LOG.info("Timeouts: " + getLatestTimeoutResults());
    }

    protected ResultsWithThroughput getLatestResults() {
        return resultsList.isEmpty() ? null : resultsList.get(resultsList.size() - 1);
    }

    protected Results getLatestTimeoutResults() {
        return timeoutResultsList.isEmpty() ? null : timeoutResultsList.get(timeoutResultsList.size() - 1);
    }

    private Throwable error = null;

    public synchronized Throwable getError() {
        return error;
    }

    public synchronized void setError(Throwable error) {
        this.error = error;
    }

    protected void pauseWithErrorChecking(long millis) {
        pauseWithErrorChecking(millis, 1000);
    }

    protected void pauseWithErrorChecking(long millis, long checkIntervals) {
        long count = millis / checkIntervals;
        long remainder = millis % checkIntervals;
        for (int i = 0; i < count + 1; i++) { // repeat 'count' times + once for remainder
            if (i < count || remainder > 0) { // on last iteration check if any remainder
                pause(checkIntervals);
                if (getError() != null) {
                    throw new RuntimeException("PerformanceTestRunnable threw an exception. Stopping the test.", getError());
                }
            }
        }
    }

    protected abstract boolean isLatestResultsWithinLimits();

    protected boolean isLatestTimeoutsWithinLimits() {
        boolean socketTimeoutsWithinLimits = true;
        for (String metric : getLatestResults().keySet()) {
            long timemoutCount = getLatestTimeoutResults().containsKey(metric) ? getLatestTimeoutResults().get(metric).getCount() : 0;
            double timeoutPercentage = (double) timemoutCount / getLatestResults().get(metric).getCount();
            socketTimeoutsWithinLimits = socketTimeoutsWithinLimits && timeoutPercentage < AVERAGE_TIMEOUT_PERCENTAGE_LIMIT;
        }
        return socketTimeoutsWithinLimits;
    }

    protected abstract Runnable newRunnable();

    public abstract class Runnable extends RepeatRunnable {

        protected final Timer timer;

        public Runnable() {
            super(SLEEP_BETWEEN_REPEATS * 1000);
            this.timer = new Timer();
        }

        @Override
        public void repeat() {
            try {
                performanceScenario();
            } catch (OperationTimeoutException ex) {
                timeouts.addValue(ex.getMetric(), ex.getValue());
                LOG.debug(String.format("Operatin %s timed out. Cause: %s.", ex.getMetric(), ex.getCause()));
            } catch (AssertionError | Exception ex) {
                setError(ex);
                throw new RuntimeException(ex);
            }
        }

        public abstract void performanceScenario() throws Exception;

    }

}
