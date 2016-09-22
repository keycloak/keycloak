package org.keycloak.testsuite.util.junit;

import org.apache.commons.configuration.PropertiesConfiguration;

import org.jboss.logging.Logger;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Aggregates jUnit test results into a single report - XML file.
 */
public class AggregateResultsReporter extends RunListener {

    private static final Logger LOGGER = Logger.getLogger(AggregateResultsReporter.class);

    private final Document xml;
    private final File reportFile;
    private final boolean working;

    private final AtomicInteger tests = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger failures = new AtomicInteger(0);
    private final AtomicInteger ignored = new AtomicInteger(0);
    private final AtomicLong suiteStartTime = new AtomicLong(0L);

    private final AtomicReference<Element> testsuite = new AtomicReference<Element>();

    private final Map<String, Long> testTimes = new HashMap<String, Long>();

    public AggregateResultsReporter() {
        boolean working = true;
        Document xml = null;
        try {
            xml = createEmptyDocument();
        } catch (ParserConfigurationException ex) {
            LOGGER.error("Failed to create XML DOM - reporting will not be done", ex);
            working = false;
        }

        File reportFile = null;
        try {
            reportFile = createReportFile();
        } catch (Exception ex) {
            LOGGER.error("Failed to create log file - reporting will not be done", ex);
            working = false;
        }

        this.working = working;
        this.xml = xml;
        this.reportFile = reportFile;
    }

    private Document createEmptyDocument() throws ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.newDocument();
    }

    private File createReportFile() throws Exception {
        String logDirPath = null;

        try {
            PropertiesConfiguration config = new PropertiesConfiguration(System.getProperty("testsuite.constants"));
            config.setThrowExceptionOnMissing(true);
            logDirPath = config.getString("log-dir");
        } catch (Exception e) {
            logDirPath = System.getProperty("project.build.directory");
            if (logDirPath == null) {
                throw new RuntimeException("Could not determine the path to the log directory.");
            }
            logDirPath += File.separator + "surefire-reports";
        }

        final File logDir = new File(logDirPath);
        logDir.mkdirs();

        final File reportFile = new File(logDir, "junit-single-report.xml").getAbsoluteFile();
        reportFile.createNewFile();

        return reportFile;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        if (working) {
            suiteStartTime.set(System.currentTimeMillis());

            Element testsuite = xml.createElement("testsuite");

            if (description.getChildren().size() == 1) {
                testsuite.setAttribute("name", safeString(description.getChildren().get(0).getDisplayName()));
            }

            xml.appendChild(testsuite);
            this.testsuite.set(testsuite);
            writeXml();
        }
    }

    @Override
    public void testStarted(Description description) throws Exception {
        if (working) {
            testTimes.put(description.getDisplayName(), System.currentTimeMillis());
        }
    }

    @Override
    public void testFinished(Description description) throws Exception {
        if (working) {
            if (testTimes.containsKey(description.getDisplayName())) {
                testsuite.get().appendChild(createTestCase(description));
                writeXml();
            }
        }
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        if (working) {
            ignored.incrementAndGet();

            Element testcase = createTestCase(failure.getDescription());
            Element skipped = xml.createElement("skipped");
            skipped.setAttribute("message", safeString(failure.getMessage()));

            testcase.appendChild(skipped);

            testsuite.get().appendChild(testcase);
            writeXml();
        }
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        if (working) {
            if (failure.getDescription().getMethodName() == null) {
                // before class failed
                for (Description child : failure.getDescription().getChildren()) {
                    // mark all methods failed
                    testFailure(new Failure(child, failure.getException()));
                }
            } else {
                // normal failure
                Element testcase = createTestCase(failure.getDescription());

                Element element;
                if (failure.getException() instanceof AssertionError) {
                    failures.incrementAndGet();
                    element = xml.createElement("failure");
                } else {
                    errors.incrementAndGet();
                    element = xml.createElement("error");
                }

                testcase.appendChild(element);

                element.setAttribute("type", safeString(failure.getException().getClass().getName()));
                element.setAttribute("message", safeString(failure.getMessage()));
                element.appendChild(xml.createCDATASection(safeString(failure.getTrace())));

                testsuite.get().appendChild(testcase);
                writeXml();
            }
        }
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        if (working) {
            ignored.incrementAndGet();

            Element testcase = createTestCase(description);

            Element skipped = xml.createElement("skipped");
            skipped.setAttribute("message", safeString(description.getAnnotation(Ignore.class).value()));

            testcase.appendChild(skipped);

            testsuite.get().appendChild(testcase);
            writeXml();
        }
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        if (working) {
            writeXml();
        }
    }

    private void writeXml() {
        Element testsuite = this.testsuite.get();

        testsuite.setAttribute("tests", Integer.toString(tests.get()));
        testsuite.setAttribute("errors", Integer.toString(errors.get()));
        testsuite.setAttribute("skipped", Integer.toString(ignored.get()));
        testsuite.setAttribute("failures", Integer.toString(failures.get()));
        testsuite.setAttribute("time", computeTestTime(suiteStartTime.get()));

        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(reportFile, false), Charset.forName("UTF-8")));
            try {
                Transformer t = TransformerFactory.newInstance().newTransformer();
                t.setOutputProperty(OutputKeys.INDENT, "yes");
                t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                t.transform(new DOMSource(xml), new StreamResult(writer));
            } catch (TransformerConfigurationException ex) {
                LOGGER.error("Misconfigured transformer", ex);
            } catch (TransformerException ex) {
                LOGGER.error("Unable to save XML file", ex);
            } finally {
                writer.close();
            }
        } catch (IOException ex) {
            LOGGER.warn("Unable to open report file", ex);
        }
    }

    private String computeTestTime(Long startTime) {
        if (startTime == null) {
            return "0";
        } else {
            long amount = System.currentTimeMillis() - startTime;
            return String.format("%.3f", amount / 1000F);
        }
    }

    private Element createTestCase(Description description) {
        tests.incrementAndGet();

        Element testcase = xml.createElement("testcase");

        testcase.setAttribute("name", safeString(description.getMethodName()));
        testcase.setAttribute("classname", safeString(description.getClassName()));
        testcase.setAttribute("time", computeTestTime(testTimes.remove(description.getDisplayName())));

        return testcase;
    }

    private String safeString(String input) {
        if (input == null) {
            return "null";
        }

        return input
                // first remove color coding (all of it)
                .replaceAll("\u001b\\[\\d+m", "")
                // then remove control characters that are not whitespaces
                .replaceAll("[\\p{Cntrl}&&[^\\p{Space}]]", "");
    }
}
