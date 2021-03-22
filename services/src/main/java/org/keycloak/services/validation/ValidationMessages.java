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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ValidationMessages {
    private Set<ValidationMessage> messages = new LinkedHashSet<>();

    public ValidationMessages() {}

    public ValidationMessages(String... messages) {
        for (String message : messages) {
            add(message);
        }
    }

    public void add(String message) {
        messages.add(new ValidationMessage(message));
    }

    public void add(String message, String localizedMessageKey) {
        messages.add(new ValidationMessage(message, localizedMessageKey));
    }

    public void add(String fieldId, String message, String localizedMessageKey) {
        ValidationMessage validationMessage = new ValidationMessage(message, localizedMessageKey);
        validationMessage.setFieldId(fieldId);
        add(validationMessage);
    }

    public void add(ValidationMessage message) {
        messages.add(message);
    }

    public boolean fieldHasError(String fieldId) {
        if (fieldId == null) {
            return false;
        }
        for (ValidationMessage message : messages) {
            if (fieldId.equals(message.getFieldId())) {
                return true;
            }
        }
        return false;
    }

    public Set<ValidationMessage> getMessages() {
        return Collections.unmodifiableSet(messages);
    }

    protected String getStringMessages(Function<? super ValidationMessage, ? extends String> function) {
        return messages.stream().map(function).collect(Collectors.joining("; "));
    }

    public String getStringMessages() {
        return getStringMessages(ValidationMessage::getMessage);
    }

    public String getStringMessages(Properties localizedMessages) {
        return getStringMessages(x -> x.getMessage(localizedMessages));
    }
}
