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

package org.keycloak.spotbugs;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

/**
 * SpotBugs detector that flags format-string mismatches in JBoss Logging calls.
 *
 * <p>JBoss Logging has two distinct method families:
 * <ul>
 *   <li>{@code *f} methods (debugf, infof, warnf, errorf, tracef, fatalf, logf) — use
 *       printf-style placeholders: {@code %s}, {@code %d}, {@code %f}, …</li>
 *   <li>{@code *v} methods (debugv, infov, warnv, errorv, tracev, fatalv, logv) — use
 *       MessageFormat-style placeholders: {@code {0}}, {@code {1}}, …</li>
 * </ul>
 *
 * <p>Two bug codes are reported:
 * <ul>
 *   <li>{@code LOGGING_FORMAT_ARG_COUNT_MISMATCH} — the number of placeholders in the
 *       format string does not match the number of arguments supplied.</li>
 *   <li>{@code LOGGING_FORMAT_WRONG_SYNTAX} — the format string uses the wrong placeholder
 *       syntax for the method variant (e.g. {@code %s} in a {@code *v} call).</li>
 * </ul>
 */
public class LoggingFormatStringDetector extends OpcodeStackDetector {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final String JBOSS_LOGGER = "org/jboss/logging/Logger";
    private static final String JBOSS_BASIC_LOGGER = "org/jboss/logging/BasicLogger";

    /**
     * Matches printf-style conversion specifiers, including %% and %n.
     * Group 1 (if present) captures an argument index (e.g. the "1" in %1$s).
     * We skip %% and %n when counting real args.
     */
    private static final Pattern PRINTF_PLACEHOLDER =
            Pattern.compile("%(?:%|(?:(\\d+)\\$)?(?:[-+# 0,(]*)(?:\\d+)?(?:\\.\\d+)?[bBhHsScCdoxXeEfgGaAtT%n])");

    /**
     * Matches MessageFormat-style placeholders: {N} and {N,…}.
     * Also matches the common typo {N] (closing bracket instead of brace) —
     * which was present in the bug that prompted this detector (issue #53069).
     * Group 1 captures the numeric index so we can find the highest index used.
     */
    private static final Pattern MSGFMT_PLACEHOLDER =
            Pattern.compile("\\{(\\d+)(?:,[^}\\]]*)?[}\\]]");

    /**
     * Map from method name to format style.
     * {@code true}  = printf-style (*f methods).
     * {@code false} = MessageFormat-style (*v methods).
     */
    private static final Map<String, Boolean> LOGGING_METHODS = new HashMap<>();

    static {
        for (String base : new String[]{"debug", "info", "warn", "error", "trace", "fatal"}) {
            LOGGING_METHODS.put(base + "f", Boolean.TRUE);
            LOGGING_METHODS.put(base + "v", Boolean.FALSE);
        }
        LOGGING_METHODS.put("logf", Boolean.TRUE);
        LOGGING_METHODS.put("logv", Boolean.FALSE);
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final BugReporter bugReporter;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public LoggingFormatStringDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    // -----------------------------------------------------------------------
    // OpcodeStackDetector contract
    // -----------------------------------------------------------------------

    @Override
    public void sawOpcode(int seen) {
        // Only care about method call instructions.
        if (seen != Const.INVOKEVIRTUAL && seen != Const.INVOKEINTERFACE) {
            return;
        }

        // Gate 1 — must be a JBoss Logger call.
        String cls = getClassConstantOperand();
        if (!JBOSS_LOGGER.equals(cls) && !JBOSS_BASIC_LOGGER.equals(cls)) {
            return;
        }

        // Gate 2 — must be one of the known *f / *v format methods.
        String methodName = getNameConstantOperand();
        Boolean isPrintf = LOGGING_METHODS.get(methodName);
        if (isPrintf == null) {
            return;
        }

        // Gate 3 — parse the method descriptor to count explicit Object arguments.
        String sig = getSigConstantOperand();
        int explicitArgs = countExplicitArgs(sig);
        if (explicitArgs < 0) {
            // Unrecognised signature shape — skip to avoid false positives.
            return;
        }

        // Gate 4 — skip varargs overloads; the array size is not statically known.
        boolean isVarargs = sig.contains("[Ljava/lang/Object;");
        if (isVarargs) {
            return;
        }

        int formatStackDepth = explicitArgs;

        // Gate 6 — retrieve the format string from the operand stack.
        OpcodeStack.Item formatItem = stack.getStackItem(formatStackDepth);
        if (formatItem == null) {
            return;
        }
        Object constant = formatItem.getConstant();
        if (!(constant instanceof String)) {
            // Non-constant format string — cannot analyse statically, skip.
            return;
        }
        String fmt = (String) constant;

        // ----------------------------------------------------------------
        // Check 1 — wrong placeholder syntax
        // ----------------------------------------------------------------
        if (!isPrintf && containsPrintfPlaceholders(fmt)) {
            // *v method but format string uses %s / %d style.
            bugReporter.reportBug(new BugInstance(this, "LOGGING_FORMAT_WRONG_SYNTAX", Priorities.NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(this)
                    .addSourceLine(this)
                    .addString(methodName + ": " + fmt));
            return;
        }
        if (isPrintf && containsMsgFmtPlaceholders(fmt)) {
            // *f method but format string uses {0} / {1} style.
            bugReporter.reportBug(new BugInstance(this, "LOGGING_FORMAT_WRONG_SYNTAX", Priorities.NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(this)
                    .addSourceLine(this)
                    .addString(methodName + ": " + fmt));
            return;
        }

        // ----------------------------------------------------------------
        // Check 2 — placeholder count vs argument count
        // ----------------------------------------------------------------
        int placeholderCount = isPrintf
                ? countPrintfPlaceholders(fmt)
                : countMsgFmtPlaceholders(fmt);

        if (placeholderCount >= 0 && placeholderCount != explicitArgs) {
            String detail = String.format(
                    "Format string has %d placeholder(s) but %d argument(s) supplied in '%s' call. Format: \"%s\"",
                    placeholderCount, explicitArgs, methodName, fmt);
            // MORE placeholders than args → will throw at runtime → HIGH priority.
            // FEWER placeholders than args → silent data loss → NORMAL priority.
            int priority = placeholderCount > explicitArgs
                    ? Priorities.HIGH_PRIORITY
                    : Priorities.NORMAL_PRIORITY;
            bugReporter.reportBug(new BugInstance(this, "LOGGING_FORMAT_ARG_COUNT_MISMATCH", priority)
                    .addClass(this)
                    .addMethod(this)
                    .addSourceLine(this)
                    .addString(detail));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Counts the number of explicit {@code Object} (or {@code String}) parameters in a
     * method descriptor, excluding the leading format-string parameter and the return type.
     *
     * <p>For example, {@code (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V}
     * returns {@code 2}.
     *
     * <p>Returns {@code -1} if the descriptor cannot be parsed (no {@code )}, no leading
     * {@code Ljava/lang/String;} format param, or otherwise unexpected shape).
     */
    private static int countExplicitArgs(String sig) {
        int closeIdx = sig.indexOf(')');
        if (closeIdx < 0) {
            return -1;
        }
        // Extract the parameters portion, e.g. "Ljava/lang/String;Ljava/lang/Object;"
        String params = sig.substring(1, closeIdx);

        // Strip the leading Level enum if present (logf/logv methods).
        if (params.startsWith("Lorg/jboss/logging/Logger$Level;")) {
            params = params.substring("Lorg/jboss/logging/Logger$Level;".length());
        }

        // Strip the leading Throwable if present.
        if (params.startsWith("Ljava/lang/Throwable;")) {
            params = params.substring("Ljava/lang/Throwable;".length());
        }

        // The first parameter must be the format String.
        if (!params.startsWith("Ljava/lang/String;")) {
            return -1;
        }
        params = params.substring("Ljava/lang/String;".length());

        if (params.isEmpty() || params.equals("[Ljava/lang/Object;")) {
            return 0;
        }

        // Count remaining typed parameters: each "L...;" or primitive or array.
        int count = 0;
        int i = 0;
        while (i < params.length()) {
            char c = params.charAt(i);
            if (c == '[') {
                // Array — skip dimensions, then process base type.
                i++;
                continue;
            } else if (c == 'L') {
                // Reference type — advance to the closing ';'.
                int semi = params.indexOf(';', i);
                if (semi < 0) {
                    return -1;
                }
                i = semi + 1;
                count++;
            } else if ("BCDFIJSZ".indexOf(c) >= 0) {
                // Primitive type.
                i++;
                count++;
            } else {
                // Unknown — bail out.
                return -1;
            }
        }
        return count;
    }

    /**
     * Returns {@code true} if the string contains at least one printf-style placeholder
     * (other than {@code %%} and {@code %n}).
     */
    private static boolean containsPrintfPlaceholders(String fmt) {
        Matcher m = PRINTF_PLACEHOLDER.matcher(fmt);
        while (m.find()) {
            String g = m.group();
            if (!"%%".equals(g) && !"%n".equals(g)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the string contains at least one MessageFormat-style
     * placeholder ({0}, {1}, …).
     */
    private static boolean containsMsgFmtPlaceholders(String fmt) {
        return MSGFMT_PLACEHOLDER.matcher(fmt).find();
    }

    /**
     * Counts printf-style placeholders in the format string, excluding {@code %%} and
     * {@code %n} which consume no arguments.
     */
    private static int countPrintfPlaceholders(String fmt) {
        Matcher m = PRINTF_PLACEHOLDER.matcher(fmt);
        int count = 0;
        while (m.find()) {
            String g = m.group();
            if ("%%".equals(g) || "%n".equals(g)) {
                continue;
            }
            if (m.group(1) != null) {
                // Argument-indexed specifier (%1$s) — cannot reliably count args statically.
                return -1;
            }
            count++;
        }
        return count;
    }

    /**
     * Counts MessageFormat-style placeholders by finding the highest index used and
     * returning {@code highestIndex + 1}.  For example, {@code "{0} and {1}"} returns
     * {@code 2}; {@code "{0} and {0}"} also returns {@code 1} (one unique slot).
     *
     * <p>Returns {@code 0} if no placeholder is found.
     */
    private static int countMsgFmtPlaceholders(String fmt) {
        Matcher m = MSGFMT_PLACEHOLDER.matcher(fmt);
        int maxIndex = -1;
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            if (idx > maxIndex) {
                maxIndex = idx;
            }
        }
        return maxIndex + 1;
    }
}
