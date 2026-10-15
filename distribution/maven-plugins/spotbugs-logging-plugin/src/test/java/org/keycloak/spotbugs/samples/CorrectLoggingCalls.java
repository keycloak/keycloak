/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.spotbugs.samples;

import org.jboss.logging.Logger;

/**
 * Sample class containing correct JBoss Logging calls.
 * The detector must produce zero bugs for all methods in this class.
 */
public class CorrectLoggingCalls {

    private static final Logger logger = Logger.getLogger(CorrectLoggingCalls.class);

    /** Correct: 2 placeholders, 2 arguments. */
    public void debugfTwoArgs() {
        logger.debugf("Loading realm %s from provider %s", "myRealm", "myProvider");
    }

    /** Correct: 1 placeholder, 1 argument. */
    public void infofOneArg() {
        logger.infof("User %s logged in", "alice");
    }

    /**
     * Correct: no placeholders, zero arguments — uses varargs overload with empty array.
     * The varargs path is skipped by the detector (cannot count statically), but this
     * is also the standard way to call a format method with no substitutions.
     */
    public void warnfNoArgs() {
        logger.warnf("Something went wrong");
    }

    /** Correct: *v method with {0} syntax and 1 argument. */
    public void debugvCorrect() {
        logger.debugv("Token expiry: {0}", "2024-01-01");
    }

    /** Correct: *v method with {0} and {1}, 2 arguments. */
    public void debugvTwoArgs() {
        logger.debugv("Realm {0} loaded from {1}", "myRealm", "myProvider");
    }

    /** Correct: 3 placeholders, 3 arguments. */
    public void errorfThreeArgs() {
        logger.errorf("Failed to load client %s in realm %s: %s", "client1", "realm1", "error msg");
    }

    /** Correct: %% is an escaped percent sign, not a placeholder — 0 real placeholders, 0 args. */
    public void debugfEscapedPercent() {
        logger.debugf("Progress: 100%% complete");
    }

    /** Correct: %n is a newline, not a placeholder — 0 real placeholders, 0 args. */
    public void debugfNewline() {
        logger.debugf("Done%n");
    }

    /** Correct: logf with Level, 1 placeholder, 1 argument. */
    public void logfCorrect() {
        logger.logf(Logger.Level.DEBUG, "User %s logged in", "alice");
    }

    /** Correct: logv with Level, {0} syntax and 1 argument. */
    public void logvCorrect() {
        logger.logv(Logger.Level.INFO, "Token expiry: {0}", "2024-01-01");
    }

    /** Correct: debugf with Throwable, 1 placeholder, 1 argument. */
    public void debugfWithThrowable() {
        logger.debugf(new RuntimeException("test"), "Error in realm %s", "myRealm");
    }
}
