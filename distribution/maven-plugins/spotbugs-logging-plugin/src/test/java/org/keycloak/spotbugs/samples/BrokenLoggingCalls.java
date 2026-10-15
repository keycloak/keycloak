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
 * Sample class containing intentionally broken JBoss Logging calls.
 * Each method exercises a different bug pattern that LoggingFormatStringDetector must detect.
 * This class is compiled and then analysed by SpotBugs in LoggingFormatStringDetectorTest.
 */
public class BrokenLoggingCalls {

    private static final Logger logger = Logger.getLogger(BrokenLoggingCalls.class);

    /** Too few args: 2 placeholders, only 1 argument supplied → LOGGING_FORMAT_ARG_COUNT_MISMATCH */
    public void tooFewArgs() {
        logger.debugf("Loading realm %s from provider %s", "myRealm");
    }

    /** Too many args: 1 placeholder, 2 arguments supplied → LOGGING_FORMAT_ARG_COUNT_MISMATCH */
    public void tooManyArgs() {
        logger.infof("User %s logged in", "alice", "extraArg");
    }

    /** No placeholder at all, but 1 argument supplied → LOGGING_FORMAT_ARG_COUNT_MISMATCH */
    public void noPlaceholderWithArg() {
        logger.warnf("Something went wrong", "ignoredArg");
    }

    /** *v method (MessageFormat) but %s syntax used → LOGGING_FORMAT_WRONG_SYNTAX */
    public void wrongSyntaxInDebugv() {
        logger.debugv("Token expiry: %s", "2024-01-01");
    }

    /** *f method (printf) but {0} syntax used → LOGGING_FORMAT_WRONG_SYNTAX */
    public void wrongSyntaxInDebugf() {
        logger.debugf("Token expiry: {0}", "2024-01-01");
    }

    /** Too few args for errorf: 2 placeholders, only 1 argument → LOGGING_FORMAT_ARG_COUNT_MISMATCH */
    public void errorfTooFew() {
        logger.errorf("Failed to load client %s in realm %s", "client1");
    }

    /**
     * Typo {1] instead of {1} in a *v call — the typo closes with ] instead of }.
     * The MSGFMT_PLACEHOLDER regex intentionally matches [}\\]] so this is detected.
     * The highest valid index is 0 (from {0}), so 1 placeholder counted but 1 arg supplied — no count mismatch.
     * However {1] is a broken placeholder: it is matched and counted, giving 2 placeholders for 1 arg
     * → LOGGING_FORMAT_ARG_COUNT_MISMATCH.
     */
    public void typoInMsgFmtBracket() {
        logger.debugv("Value of {0} is {1]", "key");
    }

    /** Too few args for logf: 2 placeholders, only 1 argument → LOGGING_FORMAT_ARG_COUNT_MISMATCH */
    public void logfTooFew() {
        logger.logf(Logger.Level.DEBUG, "Loading %s from %s", "realm1");
    }

    /** *logv method (MessageFormat) but %s syntax used → LOGGING_FORMAT_WRONG_SYNTAX */
    public void wrongSyntaxInLogv() {
        logger.logv(Logger.Level.INFO, "Token expiry: %s", "2024-01-01");
    }
}
