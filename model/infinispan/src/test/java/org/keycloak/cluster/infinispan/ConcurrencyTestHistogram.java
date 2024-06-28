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

package org.keycloak.cluster.infinispan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrencyTestHistogram {

    private final ConcurrentMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();


    public ConcurrencyTestHistogram() {

    }


    public void increaseSuccessOpsCount(long version) {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger existing = counters.putIfAbsent(version, counter);
        if (existing != null) {
            counter = existing;
        }

        counter.incrementAndGet();
    }


    public void dumpStats() {
        for (Map.Entry<Long, AtomicInteger> entry : counters.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue().get());
        }
    }
}
