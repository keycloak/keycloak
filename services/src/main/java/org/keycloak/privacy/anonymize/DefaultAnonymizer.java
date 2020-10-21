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
 * A configurable {@link Anonymizer} that obfuscates a given input according to the following rules:
 * <p>
 * Take the first {@code prefixLength} characters of the {@code input} + '%' + last chars {@code suffixLength} of a given {@code input} string.
 * If the input string is smaller than {@code minLength} chars or {@literal null} or empty, the {@code input} is returned as is.
 * <p>
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public class DefaultAnonymizer implements Anonymizer {

    /**
     * Holds the min length of input to be anonymized, shorter inputs are NOT anonymized.
     */
    private final int minLength;

    /**
     * Holds the prefix length to retain from input.
     */
    private final int prefixLength;

    /**
     * Holds the suffix length to retain from input.
     */
    private final int suffixLength;

    /**
     * Holds the placeholder to use between prefix and suffix.
     */
    private final String placeHolder;

    /**
     * Creates a new {@link DefaultAnonymizer}
     *
     * @param minLength    min length of input to be anonymized, shorter inputs are NOT anonymized
     * @param prefixLength prefix length to retain from input
     * @param suffixLength suffix length to retain from input
     * @param placeHolder  placeholder to use between prefix and suffix
     */
    public DefaultAnonymizer(int minLength,
                             int prefixLength,
                             int suffixLength,
                             String placeHolder) {
        this.minLength = minLength;
        this.prefixLength = prefixLength;
        this.suffixLength = suffixLength;
        this.placeHolder = placeHolder;
    }

    /**
     * @param input
     * @param typeHint
     * @return
     */
    public String anonymize(String input, String typeHint) {

        if (input == null) {
            return null;
        }

        int inputLen = input.length();
        if (inputLen < minLength) {
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
