package org.keycloak.gatling.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
class Stats {
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

    public long getStartTime() {
        return startTime;
    }

    public long getLastUserStart() {
        return lastUserStart;
    }

    public long getFirstUserEnd() {
        return firstUserEnd;
    }

    public long getLastUserEnd() {
        return lastUserEnd;
    }

    public Map<String, ArrayList<Long>> getCompletedIterations() {
        return completedIterations;
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
        requestCounters.put(key, count + 1);
    }

    public int requestCount(String scenario, String requestName) {
        String key = scenario + "." + requestName;
        return requestCounters.getOrDefault(key, 0);
    }
}
