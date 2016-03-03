/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import org.keycloak.hash.PasswordHashManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicy implements Serializable {

    public static final String INVALID_PASSWORD_MIN_LENGTH_MESSAGE = "invalidPasswordMinLengthMessage";
    public static final String INVALID_PASSWORD_MIN_DIGITS_MESSAGE = "invalidPasswordMinDigitsMessage";
    public static final String INVALID_PASSWORD_MIN_LOWER_CASE_CHARS_MESSAGE = "invalidPasswordMinLowerCaseCharsMessage";
    public static final String INVALID_PASSWORD_MIN_UPPER_CASE_CHARS_MESSAGE = "invalidPasswordMinUpperCaseCharsMessage";
    public static final String INVALID_PASSWORD_MIN_SPECIAL_CHARS_MESSAGE = "invalidPasswordMinSpecialCharsMessage";
    public static final String INVALID_PASSWORD_NOT_USERNAME = "invalidPasswordNotUsernameMessage";
    public static final String INVALID_PASSWORD_REGEX_PATTERN = "invalidPasswordRegexPatternMessage";
    public static final String INVALID_PASSWORD_HISTORY = "invalidPasswordHistoryMessage";

    private List<Policy> policies;
    private String policyString;

    public PasswordPolicy(String policyString) {
        this.policyString = policyString;
        this.policies = new LinkedList<>();

        if (policyString != null && !policyString.trim().isEmpty()) {
            for (String policy : policyString.split(" and ")) {
                policy = policy.trim();

                String name;
                String arg = null;

                int i = policy.indexOf('(');
                if (i == -1) {
                    name = policy.trim();
                } else {
                    name = policy.substring(0, i).trim();
                    arg = policy.substring(i + 1, policy.length() - 1);
                }

                if (name.equals(Length.NAME)) {
                    policies.add(new Length(arg));
                } else if (name.equals(Digits.NAME)) {
                    policies.add(new Digits(arg));
                } else if (name.equals(LowerCase.NAME)) {
                    policies.add(new LowerCase(arg));
                } else if (name.equals(UpperCase.NAME)) {
                    policies.add(new UpperCase(arg));
                } else if (name.equals(SpecialChars.NAME)) {
                    policies.add(new SpecialChars(arg));
                } else if (name.equals(NotUsername.NAME)) {
                    policies.add(new NotUsername(arg));
                } else if (name.equals(HashAlgorithm.NAME)) {
                    policies.add(new HashAlgorithm(arg));
                } else if (name.equals(HashIterations.NAME)) {
                    policies.add(new HashIterations(arg));
                } else if (name.equals(RegexPatterns.NAME)) {
                    Pattern.compile(arg);
                    policies.add(new RegexPatterns(arg));
                } else if (name.equals(PasswordHistory.NAME)) {
                    policies.add(new PasswordHistory(arg, this));
                } else if (name.equals(ForceExpiredPasswordChange.NAME)) {
                    policies.add(new ForceExpiredPasswordChange(arg));
                } else {
                    throw new IllegalArgumentException("Unsupported policy");
                }
            }
        }
    }

    public String getHashAlgorithm() {
        if (policies == null)
            return Constants.DEFAULT_HASH_ALGORITHM;
        for (Policy p : policies) {
            if (p instanceof HashAlgorithm) {
                return ((HashAlgorithm) p).algorithm;
            }

        }
        return Constants.DEFAULT_HASH_ALGORITHM;
    }

    /**
     *
     * @return -1 if no hash iterations setting
     */
    public int getHashIterations() {
        if (policies == null)
            return -1;
        for (Policy p : policies) {
            if (p instanceof HashIterations) {
                return ((HashIterations) p).iterations;
            }

        }
        return -1;
    }

    /**
     *
     * @return -1 if no expired passwords setting
     */
    public int getExpiredPasswords() {
        if (policies == null)
            return -1;
        for (Policy p : policies) {
            if (p instanceof PasswordHistory) {
                return ((PasswordHistory) p).passwordHistoryPolicyValue;
            }

        }
        return -1;
    }
    
    /**
    *
    * @return -1 if no force expired password change setting
    */
   public int getDaysToExpirePassword() {
       if (policies == null)
           return -1;
       for (Policy p : policies) {
           if (p instanceof ForceExpiredPasswordChange) {
               return ((ForceExpiredPasswordChange) p).daysToExpirePassword;
           }

       }
       return -1;
   }

    public Error validate(KeycloakSession session, UserModel user, String password) {
        for (Policy p : policies) {
            Error error = p.validate(session, user, password);
            if (error != null) {
                return error;
            }
        }
        return null;
    }
    
    public Error validate(KeycloakSession session, String user, String password) {
        for (Policy p : policies) {
            Error error = p.validate(session, user, password);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private static interface Policy extends Serializable {
        public Error validate(KeycloakSession session, UserModel user, String password);
        public Error validate(KeycloakSession session, String user, String password);
    }

    public static class Error {
        private String message;
        private Object[] parameters;

        private Error(String message, Object... parameters) {
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

    private static class HashAlgorithm implements Policy {
        private static final String NAME = "hashAlgorithm";
        private String algorithm;

        public HashAlgorithm(String arg) {
            algorithm = stringArg(NAME, Constants.DEFAULT_HASH_ALGORITHM, arg);
        }
        
        @Override
        public Error validate(KeycloakSession session, String user, String password) {
            return null;
        }
        
        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return null;
        }
    }

    private static class HashIterations implements Policy {
        private static final String NAME = "hashIterations";
        private int iterations;

        public HashIterations(String arg) {
            iterations = intArg(NAME, 1, arg);
        }

        @Override
        public Error validate(KeycloakSession session, String user, String password) {
            return null;
        }

        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return null;
        }
    }

    private static class NotUsername implements Policy {
        private static final String NAME = "notUsername";

        public NotUsername(String arg) {
        }

        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            return username.equals(password) ? new Error(INVALID_PASSWORD_NOT_USERNAME) : null;
        }
        
        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return validate(session, user.getUsername(), password);
        }
    }

    private static class Length implements Policy {
        private static final String NAME = "length";
        private int min;

        public Length(String arg)
        {
            min = intArg(NAME, 8, arg);
        }
        

        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            return password.length() < min ? new Error(INVALID_PASSWORD_MIN_LENGTH_MESSAGE, min) : null;
        }
        
        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return validate(session, user.getUsername(), password);
        }
    }

    private static class Digits implements Policy {
        private static final String NAME = "digits";
        private int min;

        public Digits(String arg)
        {
            min = intArg(NAME, 1, arg);
        }
        

        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (Character.isDigit(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_DIGITS_MESSAGE, min) : null;
        }
        
        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return validate(session, user.getUsername(), password);
        }
    }

    private static class LowerCase implements Policy {
        private static final String NAME = "lowerCase";
        private int min;

        public LowerCase(String arg)
        {
            min = intArg(NAME, 1, arg);
        }
        
        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (Character.isLowerCase(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_LOWER_CASE_CHARS_MESSAGE, min) : null;
        }
        
        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return validate(session, user.getUsername(), password);
        }
    }

    private static class UpperCase implements Policy {
        private static final String NAME = "upperCase";
        private int min;

        public UpperCase(String arg) {
            min = intArg(NAME, 1, arg);
        }

        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_UPPER_CASE_CHARS_MESSAGE, min) : null;
        }
        
        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return validate(session, user.getUsername(), password);
        }
    }

    private static class SpecialChars implements Policy {
        private static final String NAME = "specialChars";
        private int min;

        public SpecialChars(String arg)
        {
            min = intArg(NAME, 1, arg);
        }
        
        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            int count = 0;
            for (char c : password.toCharArray()) {
                if (!Character.isLetterOrDigit(c)) {
                    count++;
                }
            }
            return count < min ? new Error(INVALID_PASSWORD_MIN_SPECIAL_CHARS_MESSAGE, min) : null;
        }
        
        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return validate(session, user.getUsername(), password);
        }
    }

    private static class RegexPatterns implements Policy {
        private static final String NAME = "regexPattern";
        private String regexPattern;

        public RegexPatterns(String arg)
        {
            regexPattern = arg;
        }

        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            Pattern pattern = Pattern.compile(regexPattern);
            Matcher matcher = pattern.matcher(password);
            if (!matcher.matches()) {
                return new Error(INVALID_PASSWORD_REGEX_PATTERN, (Object) regexPattern);
            }
            return null;
        }

        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return validate(session, user.getUsername(), password);
        }
    }

    private static class PasswordHistory implements Policy {
        private static final String NAME = "passwordHistory";
        private final PasswordPolicy passwordPolicy;
        private int passwordHistoryPolicyValue;

        public PasswordHistory(String arg, PasswordPolicy passwordPolicy)
        {
            this.passwordPolicy = passwordPolicy;
            passwordHistoryPolicyValue = intArg(NAME, 3, arg);
        }
        
        @Override
        public Error validate(KeycloakSession session, String user, String password) {
            return null;
        }

        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            if (passwordHistoryPolicyValue != -1) {
                UserCredentialValueModel cred = getCredentialValueModel(user, UserCredentialModel.PASSWORD);
                if (cred != null) {
                    if(PasswordHashManager.verify(session, passwordPolicy, password, cred)) {
                        return new Error(INVALID_PASSWORD_HISTORY, passwordHistoryPolicyValue);
                    }
                }

                List<UserCredentialValueModel> passwordExpiredCredentials = getCredentialValueModels(user, passwordHistoryPolicyValue - 1,
                        UserCredentialModel.PASSWORD_HISTORY);
                for (UserCredentialValueModel credential : passwordExpiredCredentials) {
                    if (PasswordHashManager.verify(session, passwordPolicy, password, credential)) {
                        return new Error(INVALID_PASSWORD_HISTORY, passwordHistoryPolicyValue);
                    }
                }
            }
            return null;
        }

        private UserCredentialValueModel getCredentialValueModel(UserModel user, String credType) {
            for (UserCredentialValueModel model : user.getCredentialsDirectly()) {
                if (model.getType().equals(credType)) {
                    return model;
                }
            }

            return null;
        }

        private List<UserCredentialValueModel> getCredentialValueModels(UserModel user, int expiredPasswordsPolicyValue,
                String credType) {
            List<UserCredentialValueModel> credentialModels = new ArrayList<UserCredentialValueModel>();
            for (UserCredentialValueModel model : user.getCredentialsDirectly()) {
                if (model.getType().equals(credType)) {
                    credentialModels.add(model);
                }
            }

            Collections.sort(credentialModels, new Comparator<UserCredentialValueModel>() {
                public int compare(UserCredentialValueModel credFirst, UserCredentialValueModel credSecond) {
                    if (credFirst.getCreatedDate() > credSecond.getCreatedDate()) {
                        return -1;
                    } else if (credFirst.getCreatedDate() < credSecond.getCreatedDate()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });

            if (credentialModels.size() > expiredPasswordsPolicyValue) {
                return credentialModels.subList(0, expiredPasswordsPolicyValue);
            }
            return credentialModels;
        }
    }
    
    private static class ForceExpiredPasswordChange implements Policy {
        private static final String NAME = "forceExpiredPasswordChange";
        private int daysToExpirePassword;

        public ForceExpiredPasswordChange(String arg) {
            daysToExpirePassword = intArg(NAME, 365, arg);
        }

        @Override
        public Error validate(KeycloakSession session, String username, String password) {
            return null;
        }

        @Override
        public Error validate(KeycloakSession session, UserModel user, String password) {
            return null;
        }
    }
    
    private static int intArg(String policy, int defaultValue, String arg) {
        if (arg == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(arg);
        }
    }

    private static String stringArg(String policy, String defaultValue, String arg) {
        if (arg == null) {
            return defaultValue;
        } else {
            return arg;
        }
    }

    @Override
    public String toString() {
        return policyString;
    }
}
