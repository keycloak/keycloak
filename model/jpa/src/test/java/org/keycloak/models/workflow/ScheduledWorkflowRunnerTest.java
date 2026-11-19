/*
 * Copyright 2026 the original author or authors.
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

package org.keycloak.models.workflow;

import org.keycloak.common.util.Time;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScheduledWorkflowRunnerTest {

    @Test
    public void testWithoutPreviousRun() {
        assertEquals(Integer.MAX_VALUE, ScheduledWorkflowRunner.computeInitialDelay(0, Integer.MAX_VALUE));
    }

    @Test
    public void testMaximumInterval() {
        assertDelay(Time.currentTime(), Integer.MAX_VALUE);
    }

    @Test
    public void testPreviousRunAheadOfLocalClock() {
        assertDelay(Time.currentTime() + 60, Integer.MAX_VALUE);
    }

    @Test
    public void testOverdueRun() {
        assertDelay(Time.currentTime() - 120, 60);
    }

    @Test
    public void testOrdinaryInterval() {
        assertDelay(Time.currentTime() - 10, 60);
    }

    private static void assertDelay(int lastRun, int interval) {
        int before = Time.currentTime();
        long delay = ScheduledWorkflowRunner.computeInitialDelay(lastRun, interval);
        int after = Time.currentTime();
        long nextRun = (long) lastRun + interval;
        assertTrue("Delay must not be shortened by overflow", delay >= Math.max(0L, nextRun - after));
        assertTrue("Delay must not exceed the remaining interval", delay <= Math.max(0L, nextRun - before));
    }
}
