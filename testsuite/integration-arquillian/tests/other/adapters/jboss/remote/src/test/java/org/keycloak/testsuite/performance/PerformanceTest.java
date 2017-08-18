package org.keycloak.testsuite.performance;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * PerformanceTest.
 *
 * @author tkyjovsk
 */
public abstract class PerformanceTest extends AbstractExampleAdapterTest {
    
    public static final Logger LOG = Logger.getLogger(PerformanceTest.class);
    
    public static final Integer WARMUP_LOAD = Integer.parseInt(System.getProperty("warmup.load", "5"));
    public static final Integer WARMUP_DURATION = Integer.parseInt(System.getProperty("warmup.duration", "30"));
    
    public static final Integer INITIAL_LOAD = Integer.parseInt(System.getProperty("initial.load", "10")); // load for the first iteration
    public static final Integer LOAD_INCREASE = Integer.parseInt(System.getProperty("load.increase", "10")); // how many threads to add before each iteration
    public static final Integer LOAD_INCREASE_RATE = Integer.parseInt(System.getProperty("load.increase.rate", "2")); // how fast to add the new threads per second

    public static final Integer MEASUREMENT_DURATION = Integer.parseInt(System.getProperty("measurement.duration", "20")); // duration of one measurement iteration

    public static final Integer MAX_ITERATIONS = Integer.parseInt(System.getProperty("max.iterations", "10"));
    public static final Integer MAX_THREADS = Integer.parseInt(System.getProperty("max.threads", "1000"));
    
    public static final Integer SLEEP_BETWEEN_LOOPS = Integer.parseInt(System.getProperty("sleep.between.loops", "0"));
    public static final Integer THREADPOOL_TERMINATION_TIMEOUT = Integer.parseInt(System.getProperty("threadpool.termination.timeout", "10"));
    public static final Integer ADDITIONAL_SLEEP_AFTER = Integer.parseInt(System.getProperty("additional.sleep.after", "0"));
    
    public static final String SCENARIO_TIME = "SCENARIO";
    
    private int currentLoad;
    
    private ExecutorService executorService;
    
    protected PerformanceStatistics statistics = new PerformanceStatistics();
    protected PerformanceStatistics timeoutStatistics = new PerformanceStatistics(); // for keeping track of # of conn. timeout exceptions

    protected List<PerformanceMeasurement> measurements = new ArrayList<>();
    
    @Before
    public void before() {
        if (WARMUP_LOAD > INITIAL_LOAD) {
            throw new IllegalArgumentException("'warmup.load' cannot be larger than 'initial.load'");
        }
        
        executorService = Executors.newFixedThreadPool(MAX_THREADS);
        currentLoad = 0;
        
        statistics.clear();
        timeoutStatistics.clear();
    }
    
    @After
    public void after() throws IOException, InterruptedException {
        executorService.shutdown();
        
        LOG.info("Waiting for threadpool termination.");
        executorService.awaitTermination(THREADPOOL_TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        pause(ADDITIONAL_SLEEP_AFTER * 1000);
        
        LOG.info("Logging out all sessions.");
        testRealmResource().logoutAll();
    }
    
    @Test
    public void test() {
        
        increaseLoadBy(WARMUP_LOAD); // increase to warmup load
        warmup();
        
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            
            int loadIncrease = (i == 0)
                    ? INITIAL_LOAD - WARMUP_LOAD // increase from warmup to initial load
                    : LOAD_INCREASE; // increase load between measurements

            increaseLoadBy(loadIncrease);
            measurePerformance();
            
            if (!isThereEnoughThreadsForNextIteration(LOAD_INCREASE)) {
                LOG.warn("Threadpool capacity reached. Stopping the test.");
                break;
            }
            if (!isLatestMeasurementWithinLimits()) {
                LOG.warn("The latest measurement exceeded expected limit. Stopping the test.");
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
            LOG.info(String.format("Increasing load from %s to %s at +%s clients/s.", currentLoad, currentLoad + loadIncrease, LOAD_INCREASE_RATE));
            for (int t = 0; t < loadIncrease; t++) {
                executorService.submit(newRunnable());
                currentLoad++;
                pauseWithErrorChecking(1000 / LOAD_INCREASE_RATE);
            }
        }
    }
    
    private void measurePerformance() {
        PerformanceMeasurement measurement = new PerformanceMeasurement(currentLoad);
        statistics.reset();
        timeoutStatistics.reset();
        
        LOG.info(String.format("Measuring performance. Iteration: %s, Load: %s, Duration: %s s", measurements.size() + 1, currentLoad, MEASUREMENT_DURATION));
        
        pauseWithErrorChecking(MEASUREMENT_DURATION * 1000);
        
        measurement.setStatistics(
                statistics.snapshot(),
                timeoutStatistics.snapshot());
        measurements.add(measurement);
        
        measurement.printToCSV(getTestName());
        measurement.printToLog();
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
    
    protected void pauseWithErrorChecking(long millis, long checkDurationMillis) {
        long checkDurationMillisMin = Math.min(millis, checkDurationMillis);
        long checkCount = millis / checkDurationMillis;
        long remainder = millis % checkDurationMillis;
        LOG.debug(String.format("Pause %s ms, checking errors once per %s ms", millis, checkDurationMillisMin));
        for (int i = 0; i < checkCount + 1; i++) { // loop 'count' times + once for remainder
            if (i < checkCount || remainder > 0) { // on last iteration check if any remainder
                pause(checkDurationMillisMin);
                if (getError() != null) {
                    throw new RuntimeException("PerformanceTestRunnable threw an exception. Stopping the test.", getError());
                }
            }
        }
    }
    
    protected PerformanceMeasurement getLatestMeasurement() {
        return measurements.isEmpty() ? null : measurements.get(measurements.size() - 1);
    }
    
    protected boolean isLatestMeasurementWithinLimits() {
        return isMeasurementWithinLimits(getLatestMeasurement());
    }
    
    protected abstract boolean isMeasurementWithinLimits(PerformanceMeasurement measurement);
    
    protected abstract Runnable newRunnable();
    
    public abstract class Runnable extends LoopingRunnable {
        
        protected final Timer timer; // for timing individual operations/requests
        private final Timer scenarioTimer; // for timing the whole scenario

        public Runnable() {
            super(SLEEP_BETWEEN_LOOPS * 1000);
            this.timer = new Timer();
            this.scenarioTimer = new Timer();
        }
        
        @Override
        public void loop() {
            try {
                scenarioTimer.reset();
                performanceScenario();
                statistics.addValue(SCENARIO_TIME, scenarioTimer.getElapsedTime());
            } catch (OperationTimeoutException ex) {
                timeoutStatistics.addValue(ex.getStatistic(), ex.getValue());
                LOG.debug(String.format("Operation %s timed out. Cause: %s.", ex.getStatistic(), ex.getCause()));
            } catch (AssertionError | Exception ex) {
                setError(ex);
                throw new RuntimeException(ex);
            }
        }
        
        public abstract void performanceScenario() throws Exception;
        
    }
    
    public String getTestName() {
        return this.getClass().getSimpleName();
    }
    
}
