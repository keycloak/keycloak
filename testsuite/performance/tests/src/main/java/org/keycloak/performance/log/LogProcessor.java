package org.keycloak.performance.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * To run use the following:
 *
 *   mvn -f testsuite/integration-arquillian/tests/performance/gatling-perf exec:java -Dexec.mainClass=org.keycloak.performance.log.LogProcessor -Dexec.args="ARGUMENTS"
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class LogProcessor {

    static boolean INLAYED_INCLUDED = false;
    static boolean OUTLAYED_INCLUDED = false;

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

    public void copyPartialLog(PrintWriter output, long start, long end) throws IOException {

        LogReader reader = new LogReader(simulationLogFile);
        try {
            LogLine line;
            while ((line = reader.readLine()) != null) {

                if (line.type() == LogLine.Type.RUN) {
                    output.println(line.rawLine());
                    continue;
                }

                long startTime = line.startTime();
                long endTime = line.endTime();

                if (startTime >= start && startTime < end) {
                    if (OUTLAYED_INCLUDED) {
                        output.println(line.rawLine());
                    } else if (endTime < end) {
                        output.println(line.rawLine());
                    }
                } else if (INLAYED_INCLUDED && endTime >= start && endTime < end) {
                    output.println(line.rawLine());
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

    static class Stats {
        private long startTime;

        // timestamp at which rampUp is complete
        private long lastUserStart;

        // timestamp at which first user completed the simulation
        private long firstUserEnd;

        // timestamp at which all users completed the simulation
        private long lastUserEnd;

        // timestamps of iteration completions - when all users achieved last step of the scenario - for each scenario in the log file
        private ConcurrentHashMap<String, ArrayList<Long>> completedIterations = new ConcurrentHashMap<>();

        private LinkedHashMap<String, Set<String>> scenarioRequests = new LinkedHashMap<>();

        private HashMap<String, Integer> requestCounters = new HashMap<>();

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public void setLastUserStart(long lastUserStart) {
            this.lastUserStart = lastUserStart;
        }

        public void setFirstUserEnd(long firstUserEnd) {
            this.firstUserEnd = firstUserEnd;
        }

        public long firstUserEnd() {
            return firstUserEnd;
        }

        public void setLastUserEnd(long lastUserEnd) {
            this.lastUserEnd = lastUserEnd;
        }

        public void addIterationCompletedByAll(String scenario, long time) {
            this.completedIterations.computeIfAbsent(scenario, k -> new ArrayList<>())
                    .add(time);
        }

        public void addRequest(String scenario, String request) {
            Set<String> requests = scenarioRequests.get(scenario);
            if (requests == null) {
                requests = new LinkedHashSet<>();
                scenarioRequests.put(scenario, requests);
            }
            requests.add(request);
            incrementRequestCounter(scenario, request);
        }

        public Map<String, Set<String>> requestNames() {
            return scenarioRequests;
        }

        private void incrementRequestCounter(String scenario, String requestName) {
            String key = scenario + "." + requestName;
            int count = requestCounters.getOrDefault(key, 0);
            requestCounters.put(key, count+1);
        }

        public int requestCount(String scenario, String requestName) {
            String key = scenario + "." + requestName;
            return requestCounters.getOrDefault(key, 0);
        }
    }


    static class LogReader {

        private BufferedReader reader;

        LogReader(File file) throws FileNotFoundException {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("utf-8")));
        }

        LogLine readLine() throws IOException {
            String line = reader.readLine();
            return line != null ? new LogLine(line) : null;
        }

        void close() throws IOException {
            reader.close();
        }
    }

    static class LogLine {

        private String rawLine;
        private Type type;
        private String scenario;
        private String userId;
        private String request;
        private long start = -1;
        private long end = -1;
        private boolean ok;

        LogLine(String line) {
            rawLine = line;
        }

        String rawLine() {
            return rawLine;
        }

        Type type() {
            return type != null ? type : parse().type;
        }

        long startTime() {
            return type != null ? start : parse().start;
        }

        long endTime() {
            return type != null ? end : parse().end;
        }

        String scenario() {
            return type != null ? scenario : parse().scenario;
        }

        String userId() {
            return type != null ? userId : parse().userId;
        }

        String request() {
            return type != null ? request : parse().request;
        }

        long logTime() {
            if (type == null) {
                parse();
            }
            return type == Type.RUN || type == Type.USER_START ? start : end;
        }

        boolean ok() {
            if (type == null) {
                parse();
            }
            return type != null ? ok : parse().ok;
        }

        LogLine parse() {
            String [] cols = rawLine.split("\\t");

            if ("RUN".equals(cols[2])) {
                type = Type.RUN;
                start = Long.parseLong(cols[3]);
            } else if ("REQUEST".equals(cols[2])) {
                type = Type.REQUEST;
                scenario = cols[0];
                userId = cols[1];
                request = cols[4];
                start = Long.parseLong(cols[5]);
                end = Long.parseLong(cols[8]);
                ok = "OK".equals(cols[9]);
            } else if ("USER".equals(cols[2])) {
                if ("START".equals(cols[3])) {
                    type = Type.USER_START;
                } else if ("END".equals(cols[3])) {
                    type = Type.USER_END;
                } else {
                    throw new RuntimeException("Unknown log entry type: USER " + cols[3]);
                }
                scenario = cols[0];
                userId = cols[1];
                start = Long.parseLong(cols[4]);
                end = Long.parseLong(cols[5]);
            } else {
                throw new RuntimeException("Unknow log entry type: " + cols[3]);
            }

            return this;
        }

        enum Type {
            RUN,
            REQUEST,
            USER_START,
            USER_END
        }
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
                System.out.println("Start time: " + stats.startTime);
                System.out.println("End time: " + stats.lastUserEnd);
                System.out.println("Duration (ms): " + (stats.lastUserEnd - stats.startTime));
                System.out.println("Ramping up completes at: " + stats.lastUserStart);
                System.out.println("Ramping down starts at: " + stats.firstUserEnd);
                System.out.println();

                System.out.println("HTTP Requests:");
                for (Map.Entry<String, Set<String>> scenario: stats.requestNames().entrySet()) {
                    for (String name: scenario.getValue()) {
                        System.out.println("  [" + scenario.getKey() + "]\t" + name + "\t" + stats.requestCount(scenario.getKey(), name));
                    }
                }
                System.out.println();

                System.out.println("Times of completed iterations:");
                for (Map.Entry<String, ArrayList<Long>> ent: stats.completedIterations.entrySet()) {
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
