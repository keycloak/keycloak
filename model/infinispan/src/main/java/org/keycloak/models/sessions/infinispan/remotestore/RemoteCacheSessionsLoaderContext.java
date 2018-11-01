/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.remotestore;

import org.keycloak.models.sessions.infinispan.initializer.SessionLoader;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheSessionsLoaderContext extends SessionLoader.LoaderContext {

    // Count of hash segments for remote infinispan cache. It's by default 256 for distributed/replicated caches
    private final int ispnSegmentsCount;

    private final int sessionsPerSegment;
    private final int sessionsTotal;


    public RemoteCacheSessionsLoaderContext(int ispnSegmentsCount, int sessionsPerSegment, int sessionsTotal) {
        super(computeSegmentsCount(sessionsTotal, sessionsPerSegment, ispnSegmentsCount));
        this.ispnSegmentsCount = ispnSegmentsCount;
        this.sessionsPerSegment = sessionsPerSegment;
        this.sessionsTotal = sessionsTotal;
    }


    // Count of segments (worker iterations for distributedExecutionService executions on KC side). Each segment will be 1 worker iteration.
    // Count of segments could be lower than "ispnSegmentsCount" and depends on the size of the cache. For example if we have cache with just 500 items,
    // we don't need 256 segments and send 256 requests to remoteCache to preload thing. Instead, we will have lower number of segments (EG. 8)
    // and we will map more ispnSegments into 1 worker segment (In this case 256 / 8 = 32. So 32 ISPN segments mapped to each worker segment)
    private static int computeSegmentsCount(int sessionsTotal, int sessionsPerSegment, int ispnSegments) {
        // No support by remote ISPN cache for segments. This can happen if remoteCache is local (non-clustered)
        if (ispnSegments < 0) {
            return 1;
        }

        int seg = sessionsTotal / sessionsPerSegment;
        if (sessionsTotal % sessionsPerSegment > 0) {
            seg = seg + 1;
        }

        int seg2 = 1;
        while (seg2<seg && seg2<ispnSegments) {
            seg2 = seg2 << 1;
        }

        return seg2;
    }


    public int getIspnSegmentsCount() {
        return ispnSegmentsCount;
    }

    public int getSessionsPerSegment() {
        return sessionsPerSegment;
    }

    public int getSessionsTotal() {
        return sessionsTotal;
    }


    @Override
    public String toString() {
        return new StringBuilder("RemoteCacheSessionsLoaderContext [ ")
                .append("segmentsCount: ").append(getSegmentsCount())
                .append(", ispnSegmentsCount: ").append(ispnSegmentsCount)
                .append(", sessionsPerSegment: ").append(sessionsPerSegment)
                .append(", sessionsTotal: ").append(sessionsTotal)
                .append(" ]")
                .toString();
    }
}
