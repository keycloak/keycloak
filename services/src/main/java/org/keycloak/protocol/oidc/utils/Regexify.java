package org.keycloak.protocol.oidc.utils;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Class responsible for taking common wildcard syntax (*), and turning it into Java regex syntax
 * <p>
 * I.E.
 * foo*bar -> ^foo*.bar$
 * with-chars-need-escaping -> ^with\-chars\-need\-escaping$
 *
 * Note, that if null is passed in, it will match NOTHING.
 */
public class Regexify {

    public static final String MATCH_ANYTHING_REGEX = "^.*$";
    public static final String MATCH_NOTHING_REGEX = "1^";

    public static Function<String, String> asString = stringToRegexify -> {
        if (stringToRegexify == null) {
            return MATCH_NOTHING_REGEX;
        }
        // Per http://docs.oracle.com/javase/tutorial/essential/regex/literals.html
        // the Java regex metachars are: <([{\^-=$!|]})?*+.>
        return "^" + stringToRegexify.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)‌​\\?\\+\\.\\>]", "\\\\$0").replaceAll("\\*", ".*") + "$";
    };

    public static Function<String, Pattern> asPattern = stringToRegexify -> Pattern.compile(asString.apply(stringToRegexify));
}
