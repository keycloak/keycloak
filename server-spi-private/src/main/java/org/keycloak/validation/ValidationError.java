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

package org.keycloak.validation;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ValidationError {
    private final String fieldId;
    private final String message;
    private final String localizedMessageKey;
    private final Object[] localizedMessageParameters;

    public ValidationError(String fieldId, String message, String localizedMessageKey, Object[] localizedMessageParameters) {
        if (message == null) {
            throw new IllegalArgumentException("Message must be set");
        }

        this.fieldId = fieldId;
        this.message = message;
        this.localizedMessageKey = localizedMessageKey;
        this.localizedMessageParameters = localizedMessageParameters;
    }

    public String getFieldId() {
        return fieldId;
    }

    public String getLocalizedMessageKey() {
        return localizedMessageKey;
    }

    public Object[] getLocalizedMessageParams() {
        return localizedMessageParameters;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage(Properties messagesBundle) {
        if (getLocalizedMessageKey() != null) {
            return MessageFormat.format(messagesBundle.getProperty(getLocalizedMessageKey(), getMessage()), getLocalizedMessageParams());
        }
        else {
            return getMessage();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError error = (ValidationError) o;
        return Objects.equals(fieldId, error.fieldId) &&
                message.equals(error.message) &&
                Objects.equals(localizedMessageKey, error.localizedMessageKey) &&
                Arrays.equals(localizedMessageParameters, error.localizedMessageParameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fieldId, message, localizedMessageKey);
        result = 31 * result + Arrays.hashCode(localizedMessageParameters);
        return result;
    }
}
