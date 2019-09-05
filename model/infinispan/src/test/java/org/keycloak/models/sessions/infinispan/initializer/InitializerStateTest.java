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

package org.keycloak.models.sessions.infinispan.initializer;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoaderContext;
import org.keycloak.storage.CacheableStorageProviderModel;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InitializerStateTest {

    @Test
    public void testOfflineLoaderContext() {
        OfflinePersistentLoaderContext ctx = new OfflinePersistentLoaderContext(28, 5);
        Assert.assertEquals(ctx.getSegmentsCount(), 6);

        ctx = new OfflinePersistentLoaderContext(19, 5);
        Assert.assertEquals(ctx.getSegmentsCount(), 4);

        ctx = new OfflinePersistentLoaderContext(20, 5);
        Assert.assertEquals(ctx.getSegmentsCount(), 4);

        ctx = new OfflinePersistentLoaderContext(21, 5);
        Assert.assertEquals(ctx.getSegmentsCount(), 5);
    }


    @Test
    public void testRemoteLoaderContext() {
        assertSegmentsForRemoteLoader(0, 64, -1, 1);
        assertSegmentsForRemoteLoader(0, 64, 256, 1);
        assertSegmentsForRemoteLoader(5, 64, 256, 1);
        assertSegmentsForRemoteLoader(63, 64, 256, 1);
        assertSegmentsForRemoteLoader(64, 64, 256, 1);
        assertSegmentsForRemoteLoader(65, 64, 256, 2);
        assertSegmentsForRemoteLoader(127, 64, 256, 2);
        assertSegmentsForRemoteLoader(1000, 64, 256, 16);

        assertSegmentsForRemoteLoader(2047, 64, 256, 32);
        assertSegmentsForRemoteLoader(2048, 64, 256, 32);
        assertSegmentsForRemoteLoader(2049, 64, 256, 64);

        assertSegmentsForRemoteLoader(1000, 64, 256, 16);
        assertSegmentsForRemoteLoader(10000, 64, 256, 256);
        assertSegmentsForRemoteLoader(1000000, 64, 256, 256);
        assertSegmentsForRemoteLoader(10000000, 64, 256, 256);
    }

    private void assertSegmentsForRemoteLoader(int sessionsTotal, int sessionsPerSegment, int ispnSegmentsCount, int expectedSegments) {
        RemoteCacheSessionsLoaderContext ctx = new RemoteCacheSessionsLoaderContext(ispnSegmentsCount, sessionsPerSegment, sessionsTotal);
        Assert.assertEquals(expectedSegments, ctx.getSegmentsCount());
    }


    @Test
    public void testComputationState() {
        OfflinePersistentLoaderContext ctx = new OfflinePersistentLoaderContext(28, 5);
        Assert.assertEquals(ctx.getSegmentsCount(), 6);

        InitializerState state = new InitializerState(ctx.getSegmentsCount());

        Assert.assertFalse(state.isFinished());
        List<Integer> segments = state.getSegmentsToLoad(0, 3);
        assertContains(segments, 3, 0, 1, 2);

        state.markSegmentFinished(1);
        state.markSegmentFinished(2);
        segments = state.getSegmentsToLoad(0, 3);
        assertContains(segments, 1, 0);

        state.markSegmentFinished(0);
        state.markSegmentFinished(3);
        segments = state.getSegmentsToLoad(4, 4);
        assertContains(segments, 2, 4, 5);

        state.markSegmentFinished(4);
        state.markSegmentFinished(5);
        segments = state.getSegmentsToLoad(4, 4);
        Assert.assertTrue(segments.isEmpty());
        Assert.assertTrue(state.isFinished());
    }

    private void assertContains(List<Integer> segments, int expectedLength, int... expected) {
        Assert.assertEquals(segments.size(), expectedLength);
        for (int i : expected) {
            Assert.assertTrue(segments.contains(i));
        }
    }

    @Test
    public void testDailyTimeout() throws Exception {
        Date date = new Date(CacheableStorageProviderModel.dailyTimeout(10, 30));
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date));
        date = new Date(CacheableStorageProviderModel.dailyTimeout(17, 45));
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date));
        date = new Date(CacheableStorageProviderModel.weeklyTimeout(Calendar.MONDAY, 13, 45));
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date));
        date = new Date(CacheableStorageProviderModel.weeklyTimeout(Calendar.THURSDAY, 13, 45));
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date));
        System.out.println("----");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        date = new Date(cal.getTimeInMillis());
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date));
        date = new Date(CacheableStorageProviderModel.dailyTimeout(hour, min));
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date));
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        date = new Date(cal.getTimeInMillis());
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date));


    }
}
