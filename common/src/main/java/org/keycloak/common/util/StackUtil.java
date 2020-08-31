package org.keycloak.common.util;

import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class StackUtil {

    private static final Logger LOG = Logger.getLogger("org.keycloak.STACK_TRACE");

    /**
     * Returns string representation of the stack trace of the current call
     * without the call to the {@code getShortStackTrace} itself, and ignoring
     * usually irrelevant calls to methods in {@code sun.} and {@code java.lang.reflect}
     * packages. The stack trace ignores calls before and including the first
     * {@code org.jboss.resteasy} method, hence it usually finishes with the
     * method handling respective REST endpoint.
     *
     * Each line of the stack trace is prepended with {@code "\n    "}.
     *
     * @return If the logger {@code org.keycloak.STACK_TRACE} is set to trace
     * level, then returns stack trace, else returns empty {@link StringBuilder}
     */
    public static StringBuilder getShortStackTrace() {
        return getShortStackTrace("\n    ");
    }

    private static final Pattern IGNORED = Pattern.compile("^sun\\.|java\\.lang\\.reflect\\.");
    private static final StringBuilder EMPTY = new StringBuilder(0);

    /**
     * Returns string representation of the stack trace of the current call
     * without the call to the {@code getShortStackTrace} itself, and ignoring
     * usually irrelevant calls to methods in {@code sun.} and {@code java.lang.reflect}
     * packages. The stack trace ignores calls before and including the first
     * {@code org.jboss.resteasy} method, hence it usually finishes with the
     * method handling respective REST endpoint.
     *
     * @param prefix Prefix to prepend to every stack trace line
     * @return If the logger {@code org.keycloak.STACK_TRACE} is set to trace
     * level, then returns stack trace, else returns empty {@link StringBuilder}
     */
    public static StringBuilder getShortStackTrace(String prefix) {
        if (! isShortStackTraceEnabled()) return EMPTY;

        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = (new Throwable()).getStackTrace();
        for (int endIndex = 2; endIndex < stackTrace.length; endIndex++) {
            StackTraceElement st = stackTrace[endIndex];
            if (IGNORED.matcher(st.getClassName()).find()) {
                continue;
            }
            if (st.getClassName().startsWith("org.jboss.resteasy")) {
                break;
            }
            sb.append(prefix).append(st);
        }
        return sb;
    }

    public static boolean isShortStackTraceEnabled() {
        return LOG.isTraceEnabled();
    }
}
