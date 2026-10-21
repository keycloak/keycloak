/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import static org.keycloak.testsuite.utils.io.IOUtil.PROJECT_BUILD_DIRECTORY;

/**
 *
 * @author tkyjovsk
 */
public class Timer {

    public static final Timer DEFAULT = new Timer();

    protected final Logger log = Logger.getLogger(Timer.class);

    protected static final File DATA_DIR = new File(PROJECT_BUILD_DIRECTORY, "stats/data");
    protected static final File CHARTS_DIR = new File(PROJECT_BUILD_DIRECTORY, "stats/charts");

    public static final String DEFAULT_OPERATION = "DEFAULT_OPERATION";

    private Long time;
    private String operation = DEFAULT_OPERATION;
    private final Map<String, List<Long>> stats = new TreeMap<>();

    public long elapsedTime() {
        long elapsedTime = 0;
        if (time == null) {
        } else {
            elapsedTime = new Date().getTime() - time;
        }
        return elapsedTime;
    }

    public void reset() {
        reset(operation); // log last operation
    }

    public void reset(String operation) {
        reset(operation, true);
    }

    public void reset(String newOperation, boolean logOperationOnChange) {
        if (time != null) {
            if (operation.equals(newOperation) || logOperationOnChange) {
                logOperation(operation, elapsedTime());
            }
        }
        time = new Date().getTime();
        if (!operation.equals(newOperation)) {
            operation = newOperation;
            log.info(String.format("Operation '%s' started.", newOperation));
        }
    }

    private void logOperation(String operation, long duration) {
        if (!stats.containsKey(operation)) {
            stats.put(operation, new ArrayList<>());
        }
        stats.get(operation).add(duration);
        log.info(String.format("Operation '%s' took: %s ms", operation, duration));
    }

    public void clearStats() {
        clearStats(true, true, true);
    }

    public void clearStats(boolean logStats, boolean saveData, boolean saveCharts) {
        if (logStats) {
            log.info("Timer Statistics:");
            for (var entry : stats.entrySet()) {
                long sum = 0;
                for (Long duration : entry.getValue()) {
                    sum += duration;
                }
                log.info(String.format("Operation '%s' average: %s ms", entry.getKey(), sum / entry.getValue().size()));
            }
        }
        if (PROJECT_BUILD_DIRECTORY.exists()) {
            DATA_DIR.mkdirs();
            CHARTS_DIR.mkdirs();
            for (String op : stats.keySet()) {
                if (saveData) {
                    saveData(op);
                }
                if (saveCharts) {
                    saveChart(op);
                }
            }
        }
        stats.clear();
    }

    private void saveData(String op) {
        try {
            File f = new File(DATA_DIR, op.replace(" ", "_") + ".txt");
            if (!f.createNewFile()) {
                throw new IOException("Couldn't create file: " + f);
            }
            OutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
            for (Long duration : stats.get(op)) {
                IOUtils.write(duration.toString(), stream, StandardCharsets.UTF_8);
                IOUtils.write("\n", stream, StandardCharsets.UTF_8);
            }
            stream.flush();
            IOUtils.closeQuietly(stream);
        } catch (IOException ex) {
            log.error("Unable to save data for operation '" + op + "'", ex);
        }
    }

    private void saveChart(String op) {
        XYSeries series = new XYSeries(op);
        int i = 0;
        for (Long duration : stats.get(op)) {
            series.add(++i, duration);
        }
        final XYSeriesCollection data = new XYSeriesCollection(series);
        final JFreeChart chart = ChartFactory.createXYLineChart(
                op,
                "Operations",
                "Duration (ms)",
                data,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        try {
            ChartUtilities.saveChartAsPNG(
                    new File(CHARTS_DIR, op.replace(" ", "_") + ".png"),
                    chart, 640, 480);
        } catch (IOException ex) {
            log.warn("Unable to save chart for operation '" + op + "'.");
        }
    }

}
