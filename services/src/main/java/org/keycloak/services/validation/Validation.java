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

package org.keycloak.services.validation;

import org.keycloak.models.utils.FormMessage;
import org.keycloak.userprofile.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Validation {

    public static final String FIELD_PASSWORD_CONFIRM = "password-confirm";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_OTP_CODE = "totp";
    public static final String FIELD_OTP_LABEL = "userLabel";

    // Actually allow same emails like angular. See ValidationTest.testEmailValidation()
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*");

    private static void addError(List<FormMessage> errors, String field, String message, Object... parameters){
        errors.add(new FormMessage(field, message, parameters));
    }

    /**
     * Check if string is empty (null or lenght is 0)
     * 
     * @param s to check
     * @return true if string is empty
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
    
    /**
     * Check if string is blank (null or lenght is 0 or contains only white characters)
     * 
     * @param s to check
     * @return true if string is blank
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static boolean isEmailValid(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }


    public static List<FormMessage> getFormErrorsFromValidation(List<ValidationException.Error> errors) {
        List<FormMessage> messages = new ArrayList<>();
        for (ValidationException.Error error : errors) {
            addError(messages, error.getAttribute(), error.getMessage(), error.getMessageParameters());
        }
        return messages;

    }
}
