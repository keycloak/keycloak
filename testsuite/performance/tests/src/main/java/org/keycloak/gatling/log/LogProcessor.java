package org.keycloak.gatling.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.keycloak.performance.TestConfig.SIMPLE_TIME;

/**
 * To run use the following:
 *
 *   mvn -f testsuite/integration-arquillian/performance/tests exec:java -Dexec.mainClass=org.keycloak.performance.log.LogProcessor -Dexec.args="ARGUMENTS"
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class LogProcessor {

    boolean inlayedIncluded = false;
    boolean outlayedIncluded = false;

    File simulationLogFile;
    String lastRequestLabel;

    HashMap<String, HashMap<String, Integer>> userIterations = new HashMap<>();
    HashMap<String, Integer> currentIterations = new HashMap<>();

    /**
     * Create a log processor that knows how to parse the following format
     *
     *
     * keycloak.AdminSimulation        adminsimulation RUN     1502467483145   null    2.0
     * AdminSimulation 1829459789004783445-0   USER    START   1502467483171   0
     * AdminSimulation 1829459789004783445-0   REQUEST         Console REST - Config   1502467483296   1502467483299   1502467483303   1502467483303   OK
     * AdminSimulation 1829459789004783445-1   USER    START   1502467483328   0
     * AdminSimulation 1829459789004783445-1   REQUEST         Console Home    1502467483331   1502467483335   1502467483340   1502467483340   OK
     * AdminSimulation 1829459789004783445-2   REQUEST         Console REST - realm_0/users/ID/reset-password PUT      1502467578382   1502467578382   1502467578393   1502467578393   KO      status.find.is(204), but actually found 401
     * AdminSimulation 1829459789004783445-40  REQUEST         Console REST - realm_0  1502467578386   1502467578386   1502467578397   1502467578397   KO      status.find.is(200), but actually found 401
     * AdminSimulation 1829459789004783445-40  USER    END     1502467487280   1502467581383
     * AdminSimulation 1829459789004783445-43  REQUEST         Console REST - realm_0/users/ID/reset-password PUT      1502467581480   1502467581480   1502467581487   1502467581487   KO      status.find.is(204), but actually found 401
     * AdminSimulation 1829459789004783445-43  USER    END     1502467487581   1502467581489
     * AdminSimulation 1829459789004783445-42  REQUEST         Console REST - realm_0/users/ID/reset-password PUT      1502467582881   1502467582881   1502467582885   1502467582885   KO      status.find.is(204), but actually found 401
     */
    public LogProcessor(String logFilePath) {
        simulationLogFile = new File(logFilePath);
    }

    public LogProcessor(Class simulationClass) {
        this(getLatestSimulationLogDir(getSimulationId(simulationClass)).getAbsoluteFile().toString() + "/simulation.log");
    }

    public static String getSimulationId(Class simulationClass) {
        return simulationClass.getSimpleName().toLowerCase();
    }
    
    private static File getLatestSimulationLogDir(String simulationId) {
        String buildDirPath = System.getProperty("project.build.directory", "target");
        String resultsDirPath = System.getProperty("gatling.core.directory.results", buildDirPath + "/gatling");
        File resultsDir = new File(resultsDirPath);
        
        return Arrays.stream(resultsDir.listFiles((dir, name) -> name.startsWith(simulationId)))
                .sorted((a, b) -> -a.compareTo(b))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Latest simulation.log not found."));
    }
    
    public Stats stats() throws IOException {

        Stats stats = new Stats();

        LogReader reader = new LogReader(simulationLogFile);
        try {
            LogLine line;
            while ((line = reader.readLine()) != null) {
                if (line.type() == LogLine.Type.RUN) {
                    stats.setStartTime(line.startTime());
                } else if (line.type() == LogLine.Type.USER_START) {
                    stats.setLastUserStart(line.startTime());
                    userStarted(line.scenario(), line.userId());
                } else if (line.type() == LogLine.Type.USER_END) {
                    if (line.ok() && stats.firstUserEnd() == 0) {
                        stats.setFirstUserEnd(line.endTime());
                    }
                    stats.setLastUserEnd(line.endTime());
                    userCompleted(line.scenario(), line.userId());
                } else if (line.type() == LogLine.Type.REQUEST) {
                    String scenario = line.scenario();
                    stats.addRequest(scenario, line.request());
                    if (lastRequestLabel != null && line.request().endsWith(lastRequestLabel)) {
                        iterationCompleted(scenario, line.userId());
                        if (allUsersCompletedIteration(scenario)) {
                            advanceIteration(scenario);
                            stats.addIterationCompletedByAll(scenario, line.endTime());
                        }
                    }
                }
            }
        } finally {
            reader.close();
        }

        return stats;
    }


    private void iterationCompleted(String scenario, String userId) {
        HashMap<String, Integer> userMap = userIterations.computeIfAbsent(scenario, k -> new HashMap<>());
        int count = userMap.getOrDefault(userId, 0);
        userMap.put(userId, count + 1);
    }

    private void userStarted(String scenario, String userId) {
        HashMap<String, Integer> userMap = userIterations.computeIfAbsent(scenario, k -> new HashMap<>());
        userMap.put(userId, 0);
    }

    private void userCompleted(String scenario, String userId) {
        HashMap<String, Integer> userMap = userIterations.computeIfAbsent(scenario, k -> new HashMap<>());
        userMap.remove(userId);
    }

    private boolean allUsersCompletedIteration(String scenario) {
        HashMap<String, Integer> userMap = userIterations.computeIfAbsent(scenario, k -> new HashMap<>());
        // check if all users have reached currentIteration
        for (Integer val: userMap.values()) {
            if (val < currentIterations.getOrDefault(scenario, 1))
                return false;
        }
        return true;
    }

    private void advanceIteration(String scenario) {
        currentIterations.put(scenario, 1 + currentIterations.getOrDefault(scenario, 1));
    }

    public void filterLog(long start, long end) throws IOException {
        filterLog(start, end, inlayedIncluded, outlayedIncluded);
    }

    /**
     *
     * Setting inlayedIncluded to either true or false, results in first second anomaly towards lower values
     * Setting outlayedIncluded to true, and inlayedIncluded to false seems to behave best balancing the first second
     * downward skew with the last second upward skew. But it varies between test, and it's hard to say.
     *
     * All requests within time interval will also have their corresponding USER START and USER END entries copied
     * over, and adjusted to time interval start and end boundaries.
     *
     * @param start Time stamp at which to start copying over logged entries
     * @param end Time stamp beyond which logging entries are no longer copied over
     * @param inlayedIncluded Requests that start before interval start time but end within time interval should be included
     * @param outlayedIncluded Requests that start within time interval but end after interval end time should be included
     * @throws IOException
     */
    public void filterLog(long start, long end, boolean inlayedIncluded, boolean outlayedIncluded) throws IOException {
        this.inlayedIncluded = inlayedIncluded;
        this.outlayedIncluded = outlayedIncluded;
        
        System.out.println(String.format("Filtering %s/%s \ninlayedIncluded: %s \noutlayedIncluded: %s",
                simulationLogFile.getParentFile().getName(), simulationLogFile.getName(), this.inlayedIncluded, this.outlayedIncluded));

        File simulationLogFileFiltered = new File(simulationLogFile.getAbsoluteFile() + ".filtered");
        File simulationLogFileUnfiltered = new File(simulationLogFile.getAbsoluteFile() + ".unfiltered");

        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(simulationLogFileFiltered), "utf-8"));
        copyPartialLog(out, start, end);

        simulationLogFile.renameTo(simulationLogFileUnfiltered);
        simulationLogFileFiltered.renameTo(simulationLogFile);
    }
    
    public void copyPartialLog(PrintWriter output, long start, long end) throws IOException {

        System.out.println(String.format("Extracting from: %s to %s   (%s - %s)", SIMPLE_TIME.format(start), SIMPLE_TIME.format(end), start, end));

        HashMap<String, LogLine> starts = new HashMap<>();
        HashMap<String, long[]> sessionTimes = new HashMap<>();


        // We adjust log entires so that stats are correctly calculated
        //   - we add USER START entries for user sessions that cross the time period boundaries
        //   - we adjust start time of USER START entries to time interval start time
        //   - we adjust end time of USER END entries that occur beyond the time boundaries to time interval end time

        LogReader reader = new LogReader(simulationLogFile);
        try {
            LogLine line;
            while ((line = reader.readLine()) != null) {

                if (line.type() == LogLine.Type.ASSERTION) {
                    output.println(line.rawLine());
                    continue;
                }

                if (line.type() == LogLine.Type.RUN) {
                    // adjust start time of simulation
                    line.setStart(start);
                    output.println(line.compose());
                    continue;
                }

                long startTime = line.startTime();
                long endTime = line.endTime();


                if (line.type() == LogLine.Type.USER_END) {
                    // if it is a USER_END we should adjust it to end time of the last and start time of the first REQUEST
                    // for that session within the interval
                    long[] times = sessionTimes.remove(line.userId());

                    // We ignore USER END entries that end before interval start, and those with start time after interval end
                    if (endTime >= start && startTime < end && startTime >= start) {
                        if (times == null) {
                            throw new IllegalStateException("There should be session info present for user: " + line.userId());
                        }
                        line.setStart(times[0]);
                        // if USER END ends outside the time interval adjust it to end time of last request
                        if (line.endTime() >= end) {
                            line.setEnd(end);
                            //line.setEnd(times[1]);
                            ////line.end = times[1] > end ? times[1] : end;
                        }

                        output.println(line.compose());
                    }

                    // make sure any cached start line is also cleaned
                    starts.remove(line.userId());

                } else if (line.type() == LogLine.Type.USER_START) {
                    if (startTime < start) {
                        // if it is a USER START before the time period start, we should adjust it to start time of the first
                        // REQUEST for that session within the interval, so at this point we need to store the entry for later
                        starts.put(line.userId(), line);
                    } else if (startTime < end) {
                        // it's within the interval thus no adjustment needed
                        starts.put(line.userId(), line);
                        //output.println(line.rawLine());
                    }

                    // we ignore USER START entries beyond the time interval

                } else if (line.type() == LogLine.Type.REQUEST) {

                    // REQUEST entry needs processing if it starts within the time interval or if it ends within time interval
                    // - if we process INLAYED
                    boolean process = (startTime >= start && startTime < end) ||
                            (inlayedIncluded && endTime >= start && endTime < end);

                    if (process) {
                        // store start time of first request entry, and end time of last request entry for session within the interval
                        long[] times = sessionTimes.get(line.userId());
                        if (times == null) {
                            times = new long[]{line.startTime(), line.endTime()};
                            sessionTimes.put(line.userId(), times);

                            // if this is the first REQUEST within interval there may be a cached start line
                            LogLine startLine = starts.remove(line.userId());
                            if (startLine != null) {
                                // fix startLine's start time if necessary and recompose it writing it to output
                                if (startLine.startTime() < start) {
                                    startLine.setStart(start);
                                    //startLine.setStart(line.startTime());
                                    ////startLine.start = line.startTime() < start ? line.startTime() : start;
                                    output.println(startLine.compose());
                                } else {
                                    output.println(startLine.rawLine());
                                }
                                times[0] = startLine.startTime();
                            }
                        } else {
                            times[1] = line.endTime();
                        }

                        if (outlayedIncluded) {
                            // if entry is started within the time interval we copy it over regardless if ended outside
                            output.println(line.rawLine());
                        } else if (endTime < end) {
                            // if entry is started within the time interval and ended within a time interval we copy it over
                            output.println(line.rawLine());
                        }

                    }
                }
            }
        } finally {
            reader.close();
            output.flush();
        }
    }

    public void setLastRequestLabel(String lastRequestLabel) {
        this.lastRequestLabel = lastRequestLabel;
    }


    public static void main(String [] args) {
        if (args == null || args.length == 0) {
            printHelp();
            System.exit(1);
        }

        boolean debug = false;
        boolean help = false;
        String inFile = null;
        boolean performStat = false;
        boolean performExtract = false;
        String outFile = null;
        long startMillis = -1;
        long endMillis = -1;
        String lastRequestLabel = null;

        try {
            // Gather and print out stats
            int i = 0;
            for (i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "-X":
                        debug = true;
                        break;
                    case "-f":
                    case "--file":
                        if (i == args.length - 1) {
                            throw new RuntimeException("Argument " + arg + " requires a FILE");
                        }
                        inFile = args[++i];
                        break;
                    case "-s":
                    case "--stat":
                        performStat = true;
                        break;
                    case "-e":
                    case "--extract":
                        performExtract = true;
                        break;
                    case "-o":
                    case "--out":
                        if (i == args.length - 1) {
                            throw new RuntimeException("Argument " + arg + " requires a FILE");
                        }
                        outFile = args[++i];
                        break;
                    case "--start":
                        if (i == args.length - 1) {
                            throw new RuntimeException("Argument " + arg + " requires a timestamp in milliseconds");
                        }
                        startMillis = Long.valueOf(args[++i]);
                        break;
                    case "--end":
                        if (i == args.length - 1) {
                            throw new RuntimeException("Argument " + arg + " requires a timestamp in milliseconds");
                        }
                        endMillis = Long.valueOf(args[++i]);
                        break;
                    case "--lastRequest":
                        if (i == args.length - 1) {
                            throw new RuntimeException("Argument " + arg + " requires a LABEL");
                        }
                        lastRequestLabel = args[++i];
                        break;
                    case "--help":
                        help = true;
                        break;
                    default:
                        throw new RuntimeException("Unknown argument: " + arg);
                }
            }

            if (help) {
                printHelp();
                System.exit(0);
            }

            if (inFile == null) {
                throw new RuntimeException("No path to simulation.log file specified. Use -f FILE, or --help to see more help.");
            }

            LogProcessor proc = new LogProcessor(inFile);
            proc.setLastRequestLabel(lastRequestLabel);

            if (performStat) {
                Stats stats = proc.stats();
                // Print out results
                System.out.println("Start time: " + stats.getStartTime());
                System.out.println("End time: " + stats.getLastUserEnd());
                System.out.println("Duration (ms): " + (stats.getLastUserEnd() - stats.getStartTime()));
                System.out.println("Ramping up completes at: " + stats.getLastUserStart());
                System.out.println("Ramping down starts at: " + stats.getFirstUserEnd());
                System.out.println();

                System.out.println("HTTP Requests:");
                for (Map.Entry<String, Set<String>> scenario: stats.requestNames().entrySet()) {
                    for (String name: scenario.getValue()) {
                        System.out.println("  [" + scenario.getKey() + "]\t" + name + "\t" + stats.requestCount(scenario.getKey(), name));
                    }
                }
                System.out.println();

                System.out.println("Times of completed iterations:");
                for (Map.Entry<String, ArrayList<Long>> ent: stats.getCompletedIterations().entrySet()) {
                    System.out.println("  " + ent.getKey() + ": " + ent.getValue());
                }
            }
            if (performExtract) {
                if (outFile == null) {
                    throw new RuntimeException("No output file specified for extraction results. Use -o FILE, or --help to see more help.");
                }
                PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "utf-8"));
                proc.copyPartialLog(out, startMillis, endMillis);
            }
            if (!performStat && !performExtract) {
                throw new RuntimeException("Nothing to do. Use -s to analyze simulation log, -e to perform time based extraction, or --help to see more help.");
            }
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            if (debug) {
                t.printStackTrace();
            }
            System.exit(1);
        }
    }

    public static void printHelp() {
        System.out.println("Usage: java org.keycloak.performance.log.LogProcessor ARGUMENTS");
        System.out.println();
        System.out.println("ARGUMENTS:");
        System.out.println("  -f, --file FILE      Path to simulation.log file ");
        System.out.println("  -s, --stat           Perform analysis of the log and output some stats");
        System.out.println("  -e, --extract        Copy a portion of the file PATH_TO_SIMULATION_LOG_FILE ");
        System.out.println("  -o, --out FILE       Output file that will contain extracted portion of the log");
        System.out.println("  --start MILLIS       Timestamp at which to start extracting");
        System.out.println("  --end MILLIS         Timestamp at which to stop extracting");
        System.out.println("  --lastRequest LABEL  Label of last request in the iteration");
        System.out.println("  -X                   Output a detailed error when something goes wrong");
        System.out.println();
    }
}
