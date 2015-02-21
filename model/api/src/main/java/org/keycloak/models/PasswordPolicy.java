package org.keycloak.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicy {

    public static final String INVALID_PASSWORD_MIN_LENGTH_MESSAGE = "invalidPasswordMinLengthMessage";
    public static final String INVALID_PASSWORD_MIN_DIGITS_MESSAGE = "invalidPasswordMinDigitsMessage";
    public static final String INVALID_PASSWORD_MIN_LOWER_CASE_CHARS_MESSAGE = "invalidPasswordMinLowerCaseCharsMessage";
    public static final String INVALID_PASSWORD_MIN_UPPER_CASE_CHARS_MESSAGE = "invalidPasswordMinUpperCaseCharsMessage";
    public static final String INVALID_PASSWORD_MIN_SPECIAL_CHARS_MESSAGE = "invalidPasswordMinSpecialCharsMessage";
    public static final String INVALID_PASSWORD_NOT_USERNAME = "invalidPasswordNotUsernameMessage";
    
    private List<Policy> policies;
    private String policyString;

    public PasswordPolicy(String policyString) {
        if (policyString == null || policyString.length() == 0) {
            this.policyString = null;
            policies = Collections.emptyList();
        } else {
            this.policyString = policyString;
            policies = parse(policyString);
        }
    }

    private static List<Policy> parse(String policyString) {
        List<Policy> list = new LinkedList<Policy>();
        String[] policies = policyString.split(" and ");
        for (String policy : policies) {
            policy = policy.trim();

            String name;
            String[] args = null;

            int i = policy.indexOf('(');
            if (i == -1) {
                name = policy.trim();
            } else {
                name = policy.substring(0, i).trim();
                args = policy.substring(i + 1, policy.length() - 1).split(",");
                for (int j = 0; j < args.length; j++) {
                    args[j] = args[j].trim();
                }
            }

            if (name.equals(Length.NAME)) {
                list.add(new Length(args));
            } else if (name.equals(Digits.NAME)) {
                list.add(new Digits(args));
            } else if (name.equals(LowerCase.NAME)) {
                list.add(new LowerCase(args));
            } else if (name.equals(UpperCase.NAME)) {
                list.add(new UpperCase(args));
            } else if (name.equals(SpecialChars.NAME)) {
                list.add(new SpecialChars(args));
            } else if (name.equals(NotUsername.NAME)) {
                list.add(new NotUsername(args));
            } else if (name.equals(HashIterations.NAME)) {
                list.add(new HashIterations(args));
            }
        }
        return list;
    }

    /**
     *
     * @return -1 if no hash iterations setting
     */
    public int getHashIterations() {
        if (policies == null) return -1;
        for (Policy p : policies) {
            if (p instanceof HashIterations) {
                return ((HashIterations)p).iterations;
            }

        }
        return -1;
    }

    public Error validate(String username, String password) {
        for (Policy p : policies) {
            Error error = p.validate(username, password);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private static interface Policy {
        public Error validate(String username, String password);
    }

    public static class Error{
        private String message;
        private Object[] parameters;

        private Error(String message, Object ... parameters){
            this.message = message;
            this.parameters = parameters;
        }

        public String getMessage() {
            return message;
        }

        public Object[] getParameters() {
            return parameters;
        }
    }

    private static class HashIterations implements Policy {
        private static final String NAME = "hashIterations";
        private int iterations;

        public HashIterations(String[] args) {
            iterations = intArg(NAME, 1, args);
        }

        @Override
        public Error validate(String username, String password) {
            return null;
        }
    }

    private static class NotUsername implements Policy {
        private static final String NAME = "notUsername";

        public NotUsername(String[] args) {
        }

        @Override
        public Error validate(String username, String password) {
            return username.equals(password) ? new Error(INVALID_PASSWORD_NOT_USERNAME) : null;
        }
    }

    private static class Length implements Policy {
        private static final String NAME = "length";
        private int min;

        public Length(String[] args) {
            min = intArg(NAME, 8, args);
        }

        @Override
        public Error validate(String username, String password) {
            return password.length() < min ? new Error(INVALID_PASSWORD_MIN_LENGTH_MESSAGE, min) : null;
        }
    }

    private static class Digits implements Policy {
        private static final String NAME = "digits";
        private int min;

        public Digits(String[] args) {
            min = intArg(NAME, 1, args);
        }

        @Override
        public Error validate(String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (Character.isDigit(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_DIGITS_MESSAGE, min) : null;
        }
    }

    private static class LowerCase implements Policy {
        private static final String NAME = "lowerCase";
        private int min;

        public LowerCase(String[] args) {
            min = intArg(NAME, 1, args);
        }

        @Override
        public Error validate(String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (Character.isLowerCase(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_LOWER_CASE_CHARS_MESSAGE, min): null;
        }
    }

    private static class UpperCase implements Policy {
        private static final String NAME = "upperCase";
        private int min;

        public UpperCase(String[] args) {
            min = intArg(NAME, 1, args);
        }

        @Override
        public Error validate(String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_UPPER_CASE_CHARS_MESSAGE, min) : null;
        }
    }

    private static class SpecialChars implements Policy {
        private static final String NAME = "specialChars";
        private int min;

        public SpecialChars(String[] args) {
            min = intArg(NAME, 1, args);
        }

        @Override
        public Error validate(String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (!Character.isLetterOrDigit(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_SPECIAL_CHARS_MESSAGE, min) : null;
        }
    }

    private static int intArg(String policy, int defaultValue, String... args) {
        if (args == null || args.length == 0) {
            return defaultValue;
        } else if (args.length == 1) {
            return Integer.parseInt(args[0]);
        } else {
            throw new IllegalArgumentException("Invalid arguments to " + policy + ", expect no argument or single integer");
        }
    }

    @Override
    public String toString() {
        return policyString;
    }
}
