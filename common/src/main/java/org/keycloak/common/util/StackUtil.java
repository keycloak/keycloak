package org.keycloak.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class StackUtil {

    private static final Logger LOG = Logger.getLogger("org.keycloak.STACK_TRACE");

    private static final ConcurrentHashMap<String, Object> STACK_TRACE_OBJECTS = new ConcurrentHashMap<>();

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
    public static Object getShortStackTrace() {
        return getShortStackTrace("\n    ");
    }

    private static final Pattern IGNORED = Pattern.compile("sun\\.|java\\.(lang|util|stream)\\.|org\\.jboss\\.(arquillian|logging).|org.apache.maven.surefire|org\\.junit\\.|org.keycloak.testsuite.model.KeycloakModelTest\\.");
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
    public static Object getShortStackTrace(final String prefix) {
        if (! isShortStackTraceEnabled()) return EMPTY;

        Object res = STACK_TRACE_OBJECTS.get(prefix);
        if (res == null) {
            res = stackTraceObject(prefix);
            // Do not synchronize. We don't care if the objects in the map get overridden, they are in the end the same.
            STACK_TRACE_OBJECTS.put(prefix, res);
        }
        return res;
    }

    private static Object stackTraceObject(final String prefix) {
        return new Object() {
            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                StackTraceElement[] stackTrace = (new Throwable()).getStackTrace();
                boolean stackTraceStarted = false;
                for (int endIndex = 0; endIndex < stackTrace.length; endIndex++) {
                    StackTraceElement st = stackTrace[endIndex];
                    if (! stackTraceStarted) {
                        stackTraceStarted = (getClass().getName().equals(st.getClassName()));
                        endIndex++;
                        continue;
                    }
                    if (IGNORED.matcher(st.getClassName()).find()) {
                        continue;
                    }
                    if (st.getClassName().startsWith("org.jboss.resteasy")) {
                        break;
                    }
                    sb.append(prefix).append(st);
                }
                return sb.toString();
            }
        };
    }

    public static boolean isShortStackTraceEnabled() {
        return LOG.isTraceEnabled();
    }
}
