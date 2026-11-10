/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa.session;

import org.keycloak.models.utils.SessionTimeoutHelper;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmptyRealmWatermarksTest {

    private static final int WINDOW = SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

    @Test
    public void skipsUntilDeadlineDerivedFromCurrentIdle() {
        EmptyRealmWatermarks watermarks = new EmptyRealmWatermarks();
        assertFalse(watermarks.canSkip("realm", true, 1000, 600));

        watermarks.markEmpty("realm", true, 1000);
        assertTrue(watermarks.canSkip("realm", true, 1000 + 600 + WINDOW - 1, 600));
        assertFalse(watermarks.canSkip("realm", true, 1000 + 600 + WINDOW, 600));

        // a shorter idle timeout configured later moves the deadline closer without any invalidation
        assertFalse(watermarks.canSkip("realm", true, 1000 + 100 + WINDOW, 100));
    }

    @Test
    public void entriesAreSeparatedByRealmAndType() {
        EmptyRealmWatermarks watermarks = new EmptyRealmWatermarks();
        watermarks.markEmpty("realm", true, 1000);
        assertFalse(watermarks.canSkip("realm", false, 1001, 600));
        assertFalse(watermarks.canSkip("other", true, 1001, 600));

        watermarks.clear("realm", true);
        assertFalse(watermarks.canSkip("realm", true, 1001, 600));
    }

    @Test
    public void doesNotSkipWhenClockMovedBackwards() {
        EmptyRealmWatermarks watermarks = new EmptyRealmWatermarks();
        watermarks.markEmpty("realm", false, 5000);
        assertFalse(watermarks.canSkip("realm", false, 1000, 600));
    }
}
