package org.keycloak.models.sessions.infinispan.initializer;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InitializerState extends SessionEntity {

    private static final Logger log = Logger.getLogger(InitializerState.class);

    private int sessionsCount;
    private List<Boolean> segments = new ArrayList<>();


    public void init(int sessionsCount, int sessionsPerSegment) {
        this.sessionsCount = sessionsCount;

        int segmentsCount = sessionsCount / sessionsPerSegment;
        if (sessionsPerSegment * segmentsCount < sessionsCount) {
            segmentsCount = segmentsCount + 1;
        }

        // TODO: trace
        log.info(String.format("sessionsCount: %d, sessionsPerSegment: %d, segmentsCount: %d", sessionsCount, sessionsPerSegment, segmentsCount));

        for (int i=0 ; i<segmentsCount ; i++) {
            segments.add(false);
        }
    }

    // Return true just if computation is entirely finished (all segments are true)
    public boolean isFinished() {
        return getNextUnfinishedSegmentFromIndex(0) == -1;
    }

    // Return next un-finished segments. It can return "segmentCount" segments or less
    public List<Integer> getUnfinishedSegments(int segmentCount) {
        List<Integer> result = new ArrayList<>();
        boolean remaining = true;
        int next=0;
        while (remaining && result.size() < segmentCount) {
            next = getNextUnfinishedSegmentFromIndex(next);
            if (next == -1) {
                remaining = false;
            } else {
                result.add(next);
                next++;
            }
        }

        return result;
    }

    public void markSegmentFinished(int index) {
        segments.set(index, true);
    }

    private int getNextUnfinishedSegmentFromIndex(int index) {
        int segmentsSize = segments.size();
        for (int i=index ; i<segmentsSize ; i++) {
            Boolean entry = segments.get(i);
            if (!entry) {
                return i;
            }
        }

        return -1;
    }

    public String printState(boolean includeSegments) {
        int finished = 0;
        int nonFinished = 0;
        List<Integer> finishedList = new ArrayList<>();
        List<Integer> nonFinishedList = new ArrayList<>();

        int size = segments.size();
        for (int i=0 ; i<size ; i++) {
            Boolean done = segments.get(i);
            if (done) {
                finished++;
                if (includeSegments) {
                    finishedList.add(i);
                }
            } else {
                nonFinished++;
                if (includeSegments) {
                    nonFinishedList.add(i);
                }
            }
        }

        StringBuilder strBuilder = new StringBuilder("sessionsCount: " + sessionsCount)
                .append(", finished segments count: " + finished)
                .append(", non-finished segments count: " + nonFinished);

        if (includeSegments) {
            strBuilder.append(", finished segments: " + finishedList)
                    .append(", non-finished segments: " + nonFinishedList);
        }

        return strBuilder.toString();
    }
}
