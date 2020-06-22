/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.privacy.anonymize;

/**
 * Allows to anonymize sensitive input with potentially personal identifiable information (PII).
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public interface Anonymizer {

    // Note: fields are defined as strings instead of an enum, to ease adding own fields in custom implementations.

    /**
     * Denotes a USER_ID field
     */
    String USER_ID = "userId";

    /**
     * Denotes an UP_ADDRESS field
     */
    String IP_ADDRESS = "ipAddress";

    /**
     * Denotes an USERNAME field
     */
    String USERNAME = "username";

    /**
     * Denotes an EMAIL address field
     */
    String EMAIL = "email";

    /**
     * Denotes an PHONE_NUMBER field
     */
    String PHONE_NUMBER = "phoneNumber";

    /**
     * Denotes an MOBILE field
     */
    String MOBILE = "mobile";

    /**
     * Placeholder for missing field information
     */
    String NULL = "null";

    /**
     * Anonymizes the given input string according to the rules provided for the given field.
     *
     * @param field
     * @param input
     * @return
     */
    String anonymize(String field, String input);

}
