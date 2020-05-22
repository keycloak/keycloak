package org.keycloak.events.log;

/**
 * TODO provide a reasonable default Anonymizer.
 *
 * Allows to anonymize personal identifiable values
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public interface Anonymizer {

    String USER_ID = "userId";

    String IP_ADDRESS = "ipAddress";

    String USERNAME = "username";

    String EMAIL = "email";

    String PHONE = "phoneNumber";

    String MOBILE = "mobile";

    /**
     * Anonymizes the given input string according to the rules provided for the given type.
     *
     * @param type
     * @param input
     * @return
     */
    String anonymize(String type, String input);

    /**
     * Default {@link Anonymizer}.
     */
    class Default implements Anonymizer {

        public static final int MIN_LENGTH = 6;

        /**
         * The anonymization rule is: first 2 characters + '%' + last chars 3 of a given input string.
         * If the input string is smaller than 6 chars or null or empty, the input is returned as is.
         * <p>
         * The anonymization is applied if the supplied key is one of:
         * <ul>
         *     <li>userId</li>
         *     <li>ipAddress</li>
         *     <li>username</li>
         *     <li>email</li>
         *     <li>phoneNumber</li>
         *     <li>mobile</li>
         * </ul>
         *
         * @param type
         * @param input
         * @return
         */
        public String anonymize(String type, String input) {

            if (type == null || type.isEmpty() || input == null || input.isEmpty()) {
                return input;
            }

            int inputLen = input.length();
            if (inputLen < MIN_LENGTH) {
                return input;
            }

            switch (type) {
                case USER_ID:
                case USERNAME:
                case EMAIL:
                case PHONE:
                case MOBILE:
                case IP_ADDRESS:
                    return input.substring(0, 2) + "%" + input.substring(inputLen - 3);
                default:
                    return input;
            }
        }
    }
}
