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

package org.keycloak.models.utils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionTimeoutHelper {


    /**
     * Interval specifies maximum time, for which the "userSession.lastSessionRefresh" may contain stale value.
     *
     * For example, if there are 2 datacenters and sessionRefresh will happen on DC1, then the message about the updated lastSessionRefresh may
     * be sent to the DC2 later (EG. Some periodic thread will send the updated lastSessionRefresh times in batches with 60 seconds delay).
     */
    public static final int PERIODIC_TASK_INTERVAL_SECONDS = 60;


    /**
     * The maximum time difference, which will be still tolerated when checking userSession idle timeout.
     *
     * For example, if there are 2 datacenters and sessionRefresh happened on DC1, then we still want to tolerate some timeout on DC2 due the
     * fact that lastSessionRefresh of current userSession may be updated later from DC1.
     *
     * See {@link #PERIODIC_TASK_INTERVAL_SECONDS}
     */
    public static final int IDLE_TIMEOUT_WINDOW_SECONDS = 120;


    /**
     * The maximum time difference, which will be still tolerated when checking userSession idle timeout with periodic cleaner threads.
     *
     * Just the sessions, with the timeout bigger than this value are considered really time-outed and can be garbage-collected (Considering the cross-dc
     * environment and the fact that some session updates on different DC can be postponed and seen on current DC with some delay).
     *
     * See {@link #PERIODIC_TASK_INTERVAL_SECONDS} and {@link #IDLE_TIMEOUT_WINDOW_SECONDS}
     */
    public static final int PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS = 180;
}
