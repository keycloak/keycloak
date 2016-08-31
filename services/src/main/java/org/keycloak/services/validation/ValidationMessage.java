/*
 *
 *  * Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.services.validation;

import java.text.MessageFormat;
import java.util.Properties;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ValidationMessage {
    private String fieldId;
    private String message;
    private String localizedMessageKey;
    private Object[] localizedMessageParameters;

    public ValidationMessage(String message) {
        this.message = message;
    }

    public ValidationMessage(String message, String localizedMessageKey, Object... localizedMessageParameters) {
        this.message = message;
        this.localizedMessageKey = localizedMessageKey;
        this.localizedMessageParameters = localizedMessageParameters;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String getLocalizedMessageKey() {
        return localizedMessageKey;
    }

    public void setLocalizedMessageKey(String localizedMessageKey) {
        this.localizedMessageKey = localizedMessageKey;
    }

    public Object[] getLocalizedMessageParameters() {
        return localizedMessageParameters;
    }

    public void setLocalizedMessageParameters(Object[] localizedMessageParameters) {
        this.localizedMessageParameters = localizedMessageParameters;
    }

    public String getMessage() {
        return message;
    }

    public String getMessage(Properties localizedMessages) {
        if (getLocalizedMessageKey() != null) {
            return MessageFormat.format(localizedMessages.getProperty(getLocalizedMessageKey(), getMessage()), getLocalizedMessageParameters());
        }
        else {
            return getMessage();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidationMessage message1 = (ValidationMessage) o;

        if (getFieldId() != null ? !getFieldId().equals(message1.getFieldId()) : message1.getFieldId() != null)
            return false;
        return getMessage() != null ? getMessage().equals(message1.getMessage()) : message1.getMessage() == null;

    }

    @Override
    public int hashCode() {
        int result = getFieldId() != null ? getFieldId().hashCode() : 0;
        result = 31 * result + (getMessage() != null ? getMessage().hashCode() : 0);
        return result;
    }
}
