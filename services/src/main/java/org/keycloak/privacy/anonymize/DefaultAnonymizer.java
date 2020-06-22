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

import java.util.Set;

/**
 * A configurable {@link Anonymizer} that allows to filter values according to configured anonymization policy.
 * <p>
 * An example for an anonymization policy is:
 * Take the first {@code prefixLength} characters of the {@code input} + '%' + last chars {@code suffixLength} of a given {@code input} string.
 * If the input string is smaller than {@code minLength} chars or {@literal null} or empty, the {@code input} is returned as is.
 * <p>
 * The anonymization could be applied if the supplied key is one of:
 * <ul>
 *     <li>userId</li>
 *     <li>ipAddress</li>
 *     <li>username</li>
 *     <li>email</li>
 *     <li>phoneNumber</li>
 *     <li>mobile</li>
 *     <li>null</li>
 * </ul>
 *  Note that th fieldname {@code null} is used to control default handling for unknown fields.
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public class DefaultAnonymizer implements Anonymizer {

    private final int minLength;

    private final int prefixLength;

    private final int suffixLength;

    private final String placeHolder;

    private final Set<String> fields;

    private final String fallbackField;

    /**
     * Creates a new {@link DefaultAnonymizer}
     *
     * @param minLength min length of input to be anonymized
     * @param prefixLength prefix length to keep from input
     * @param suffixLength suffix length to keep from input
     * @param placeHolder placeholder to use between prefix and suffix
     * @param fields set of fields that should be anonymized
     * @param fallbackField field that should be used if no field is provided
     */
    public DefaultAnonymizer(int minLength,
                             int prefixLength,
                             int suffixLength,
                             String placeHolder,
                             Set<String> fields,
                             String fallbackField) {
        this.minLength = minLength;
        this.prefixLength = prefixLength;
        this.suffixLength = suffixLength;
        this.placeHolder = placeHolder;
        this.fields = fields;
        this.fallbackField = fallbackField;
    }

    /**
     * @param field
     * @param input
     * @return
     */
    public String anonymize(String field, String input) {

        if (field == null) {
            field = fallbackField;
        }

        if (field == null || field.isEmpty() || input == null || input.isEmpty()) {
            return input;
        }

        int inputLen = input.length();
        if (inputLen < minLength) {
            return input;
        }

        if (!fields.contains(field)) {
            return input;
        }

        // allow to just return placeholder for all fields
        if (prefixLength == 0 && suffixLength == 0) {
            return placeHolder;
        }

        String prefix = input.substring(0, prefixLength);
        String suffix = input.substring(inputLen - suffixLength);
        return prefix + placeHolder + suffix;
    }
}
