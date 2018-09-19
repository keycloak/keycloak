package org.keycloak.testsuite.performance;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.keycloak.testsuite.performance.statistics.SimpleStatistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.keycloak.testsuite.performance.PerformanceTest.LOG;
import static org.keycloak.testsuite.utils.io.IOUtil.PROJECT_BUILD_DIRECTORY;

/**
 *
 * @author tkyjovsk
 */
public class PerformanceMeasurement {

    private final Date started;
    private final int load;
    private long durationMillis;
    private SimpleStatistics statistics;
    private SimpleStatistics timeoutStatistics;

    public static final DateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX"); // should be compatible with `date --iso-8601=seconds`
    public static final DateFormat RFC3339_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ"); // should be compatible with `date --rfc-3339=seconds`

    public PerformanceMeasurement(int load) {
        this.started = new Date();
        this.load = load;
    }

    public SimpleStatistics getStatistics() {
        return this.statistics;
    }

    public SimpleStatistics getTimeoutStatistics() {
        return this.timeoutStatistics;
    }

    public void setStatistics(SimpleStatistics statistics, SimpleStatistics timeoutStatistics) {
        this.durationMillis = new Date().getTime() - started.getTime();
        if (durationMillis < 0) {
            throw new IllegalStateException("Cannot set a negative duration.");
        }
        this.statistics = statistics;
        this.timeoutStatistics = timeoutStatistics;
    }

    private void checkStatisticsNotNull() {
        if (statistics == null || timeoutStatistics == null) {
            throw new IllegalStateException("Iteration doesn't have any statistics set.");
        }
    }

    public double getThroughput(String statistic) {
        checkStatisticsNotNull();
        return (double) statistics.get(statistic).getCount() / durationMillis * 1000;
    }

    public double getTimeoutPercentage(String statistic) {
        checkStatisticsNotNull();
        long timeouts = timeoutStatistics.containsKey(statistic) ? timeoutStatistics.get(statistic).getCount() : 0;
        return (double) timeouts / statistics.get(statistic).getCount();
    }

    public static final Object[] HEADER = new String[]{
        "Timestamp",
        "Load",
        "Duration",
        "Count",
        "Min",
        "Max",
        "Average",
        "Standard Deviation",
        "Timeout Percentage",
        "Throughput",};

    public List toRecord(String statistic) {
        checkStatisticsNotNull();
        List record = new ArrayList();
        record.add(ISO8601_DATE_FORMAT.format(started));
        record.add(load);
        record.add(durationMillis);
        record.add(statistics.get(statistic).getCount());
        record.add(statistics.get(statistic).getMin());
        record.add(statistics.get(statistic).getMax());
        record.add(statistics.get(statistic).getAverage());
        record.add(statistics.get(statistic).getStandardDeviation());
        record.add(getTimeoutPercentage(statistic));
        record.add(getThroughput(statistic));
        return record;
    }

    public void printToCSV() {
        printToCSV(null);
    }

    public void printToCSV(String testName) {
        checkStatisticsNotNull();
        for (String statistic : statistics.keySet()) {

            File csvFile = new File(PROJECT_BUILD_DIRECTORY + "/measurements" + (testName == null ? "" : "/" + testName),
                    statistic + ".csv");
            boolean csvFileCreated = false;
            if (!csvFile.exists()) {
                try {
                    csvFile.getParentFile().mkdirs();
                    csvFileCreated = csvFile.createNewFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, true))) {

                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180);

                if (csvFileCreated) {
                    printer.printRecord(HEADER);
                }
                printer.printRecord(toRecord(statistic));

                printer.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void printToLog() {
        LOG.info("Measurement results:");
        LOG.info("Operation " + Arrays.toString(HEADER));
        for (String statistic : statistics.keySet()) {
            LOG.info(statistic + " " + toRecord(statistic));
        }
    }

}
