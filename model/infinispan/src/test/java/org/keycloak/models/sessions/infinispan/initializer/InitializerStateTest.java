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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InitializerStateTest {

    @Test
    public void testComputationState() {
        InitializerState state = new InitializerState();
        state.init(28, 5);

        Assert.assertFalse(state.isFinished());
        List<Integer> segments = state.getUnfinishedSegments(3);
        assertContains(segments, 3, 0, 1, 2);

        state.markSegmentFinished(1);
        state.markSegmentFinished(2);
        segments = state.getUnfinishedSegments(4);
        assertContains(segments, 4, 0, 3, 4, 5);

        state.markSegmentFinished(0);
        state.markSegmentFinished(3);
        segments = state.getUnfinishedSegments(4);
        assertContains(segments, 2, 4, 5);

        state.markSegmentFinished(4);
        state.markSegmentFinished(5);
        segments = state.getUnfinishedSegments(4);
        Assert.assertTrue(segments.isEmpty());
        Assert.assertTrue(state.isFinished());
    }

    private void assertContains(List<Integer> segments, int expectedLength, int... expected) {
        Assert.assertEquals(segments.size(), expectedLength);
        for (int i : expected) {
            Assert.assertTrue(segments.contains(i));
        }
    }
}
