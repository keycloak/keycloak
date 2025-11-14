package org.keycloak.utils;

import java.net.IDN;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.Config;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Email Validator Utility to check email inputs based on
 * <a href="https://github.com/hibernate/hibernate-validator/blob/8.0.1.Final/engine/src/main/java/org/hibernate/validator/internal/constraintvalidators/AbstractEmailValidator.java">
 * hibernate-validator implementation</a>.
 */
public class EmailValidationUtil {

    public static final int MAX_LOCAL_PART_LENGTH = 64;

    private static final String LOCAL_PART_ATOM = "[a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uFFFF-]";
    private static final String LOCAL_PART_INSIDE_QUOTES_ATOM = "(?:[a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\u0080-\uFFFF-]|\\\\\\\\|\\\\\\\")";
    /**
     * Regular expression for the local part of an email address (everything before '@')
     */
    private static final Pattern LOCAL_PART_PATTERN = Pattern.compile(
            "(?:" + LOCAL_PART_ATOM + "+|\"" + LOCAL_PART_INSIDE_QUOTES_ATOM + "+\")" +
                    "(?:\\." + "(?:" + LOCAL_PART_ATOM + "+|\"" + LOCAL_PART_INSIDE_QUOTES_ATOM + "+\")" + ")*", CASE_INSENSITIVE
    );
    private static final int MAX_DOMAIN_PART_LENGTH = 255;
    private static final String DOMAIN_CHARS_WITHOUT_DASH = "[a-z\u0080-\uFFFF0-9!#$%&'*+/=?^`{|}~]";
    private static final String DOMAIN_LABEL = "(?:" + DOMAIN_CHARS_WITHOUT_DASH + "-*)*" + DOMAIN_CHARS_WITHOUT_DASH + "+";
    private static final String DOMAIN = DOMAIN_LABEL + "+(?:\\." + DOMAIN_LABEL + "+)*";
    private static final String IP_DOMAIN = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
    private static final String IP_V6_DOMAIN = "(?:(?:[0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,7}:|(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}|(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}|(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}|(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:(?:(?::[0-9a-fA-F]{1,4}){1,6})|:(?:(?::[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(?::[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(?:ffff(:0{1,4}){0,1}:){0,1}(?:(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])|(?:[0-9a-fA-F]{1,4}:){1,4}:(?:(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";
    /**
     * Regular expression for the domain part of an email address (everything after '@')
     */
    private static final Pattern EMAIL_DOMAIN_PATTERN = Pattern.compile(DOMAIN + "|\\[" + IP_DOMAIN + "\\]|" + "\\[IPv6:" + IP_V6_DOMAIN + "\\]", CASE_INSENSITIVE);

    public static final String MAX_EMAIL_LOCAL_PART_LENGTH = "max-email-local-part-length";


    public static boolean isValidEmail(String value) {
        return isValidEmail(value, Config.scope("user-profile", "declarative-user-profile").getInt(MAX_EMAIL_LOCAL_PART_LENGTH, MAX_LOCAL_PART_LENGTH));
    }

    public static boolean isValidEmail(String value, int maxEmailLocalPartLength) {
        if ( value == null || value.length() == 0 ) {
            return false;
        }

        // cannot split email string at @ as it can be a part of quoted local part of email.
        // so we need to split at a position of last @ present in the string:
        String stringValue = value.toString();
        int splitPosition = stringValue.lastIndexOf( '@' );

        // need to check if
        if ( splitPosition < 0 ) {
            return false;
        }

        String localPart = stringValue.substring( 0, splitPosition );
        String domainPart = stringValue.substring( splitPosition + 1 );

        if ( !isValidEmailLocalPart( localPart, maxEmailLocalPartLength ) ) {
            return false;
        }

        return isValidEmailDomainAddress( domainPart );
    }

    private static boolean isValidEmailLocalPart(String localPart, int maxEmailLocalPartLength) {

        if ( localPart.length() >  maxEmailLocalPartLength) {
            return false;
        }
        Matcher matcher = LOCAL_PART_PATTERN.matcher( localPart );
        return matcher.matches();
    }

    private static boolean isValidEmailDomainAddress(String domain) {
        // if we have a trailing dot the domain part we have an invalid email address.
        // the regular expression match would take care of this, but IDN.toASCII drops the trailing '.'
        if ( domain.endsWith( "." ) ) {
            return false;
        }

        String asciiString;
        try {
            asciiString = IDN.toASCII( domain );
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        if ( asciiString.length() > MAX_DOMAIN_PART_LENGTH ) {
            return false;
        }

        Matcher matcher = EMAIL_DOMAIN_PATTERN.matcher( domain );
        if ( !matcher.matches() ) {
            return false;
        }

        return true;
    }
}
