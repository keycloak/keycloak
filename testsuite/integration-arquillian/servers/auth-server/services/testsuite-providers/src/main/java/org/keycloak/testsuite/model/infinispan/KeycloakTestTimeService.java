/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.model.infinispan;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.infinispan.util.EmbeddedTimeService;
import org.keycloak.common.util.Time;

/**
 * Infinispan TimeService, which delegates to Keycloak Time.currentTime to figure current time. Useful for testing purposes.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakTestTimeService extends EmbeddedTimeService {

    private long getCurrentTimeMillis() {
        return Time.currentTimeMillis();
    }

    @Override
    public long wallClockTime() {
        return getCurrentTimeMillis();
    }

    @Override
    public long time() {
        return TimeUnit.MILLISECONDS.toNanos(getCurrentTimeMillis());
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(getCurrentTimeMillis());
    }
}
