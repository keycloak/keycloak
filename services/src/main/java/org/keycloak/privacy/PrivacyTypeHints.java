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
package org.keycloak.privacy;

/**
 * Provides some constants to use as a hint for customizing the obfuscation of Personal data.
 *
 * <a href="https://techgdpr.com/blog/difference-between-pii-and-personal-data/">What is personal data under GDPR?</a>
 */
public interface PrivacyTypeHints {

    // Note: fields are defined as strings instead of an enum, to ease adding own fields in custom implementations.

    // The following fields can be used by consumers to classify and filer their own PII values.

    /**
     * Denotes an PII type for other personally identifiable information.
     */
    String PII = "pii";

    /**
     * Denotes a USER_ID type
     */
    String USER_ID = "userId";

    /**
     * Denotes an USERNAME type
     */
    String USERNAME = "username";

    /**
     * Denotes an EMAIL address type
     */
    String EMAIL = "email";

    /**
     * Denotes an NAME type, e.g. a given name, middle name, family name.
     */
    String NAME = "name";

    /**
     * Denotes an PHONE_NUMBER type, e.g. mobile, phone
     */
    String PHONE_NUMBER = "phoneNumber";

    /**
     * Denotes an ADDRESS type
     */
    String ADDRESS = "address";

    /**
     * Denotes a PERSONAL_DATA type for generic personal data
     */
    String PERSONAL_DATA = "personalData";

    /**
     * Denotes an IP_ADDRESS type
     */
    String IP_ADDRESS = "ipAddress";

    /**
     * Denotes an DEVICE_ID type
     */
    String DEVICE_ID = "deviceId";

    /**
     * Denotes an unspecified default type
     */
    String DEFAULT = "default";

    /**
     * Special type-hint which denotes, that the input should be rendered plain without filtering.
     * This can be used to exclude fields / type-hints from filtering.
     */
    String PLAIN = "plain";
}
