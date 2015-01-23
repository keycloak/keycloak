package org.keycloak.services.cors;

import static java.util.regex.Pattern.compile;

import java.util.regex.Pattern;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class CorsUtil {
    
    /**
     * Must match "token", 1 or more of any US-ASCII char except control
     * chars or specific "separators", see:
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
     * and
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec2.html#sec2
     *
     * Note use of regex character class subtraction and character class
     * metacharacter rules.
     */
    private static final Pattern VALID = compile("^[\\x21-\\x7e&&[^]\\[}{()<>@,;:\\\\\"/?=]]+$");
    
    /**
     * Parses a header value consisting of zero or more space / comma /
     * space + comma separated strings. The input string is trimmed before
     * splitting.
     *
     * @param headerValue The header value, may be {@code null}.
     *
     * @return A string array of the parsed string items, empty if none
     *         were found or the input was {@code null}.
     */
    public static String[] parseMultipleHeaderValues(final String headerValue) {

        if (headerValue == null)
            return new String[0]; // empty array

        String trimmedHeaderValue = headerValue.trim();

        if (trimmedHeaderValue.isEmpty())
            return new String[0];

        return trimmedHeaderValue.split("\\s*,\\s*|\\s+");
    }
    
    /**
     * Applies a {@code Aaa-Bbb-Ccc} format to a header name.
     *
     * @param name The header name to format, must not be an empty string
     *             or {@code null}.
     *
     * @return The formatted header name.
     *
     * @throws IllegalArgumentException On a empty or invalid header name.
     */
    public static String formatCanonical(final String name) {

        String nameTrimmed = name.trim();

        if (nameTrimmed.isEmpty())
            throw new IllegalArgumentException("The header field name must not be an empty string");

        assert(VALID != null);

        // Check for valid syntax
        if (! VALID.matcher(nameTrimmed).matches())
            throw new IllegalArgumentException("Invalid header field name syntax (see RFC 2616)");


        String[] tokens = nameTrimmed.toLowerCase().split("-");

        String out = "";

        for (int i = 0; i < tokens.length; i++) {

            char[] c = tokens[i].toCharArray();

            // Capitalise first char
            c[0] = Character.toUpperCase(c[0]);

            if (i >= 1)
                out = out + "-";

            out = out + new String(c);
        }

        return out;
    }
}
