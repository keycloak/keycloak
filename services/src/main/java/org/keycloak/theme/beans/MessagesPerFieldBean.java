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
package org.keycloak.theme.beans;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.forms.login.MessageType;

/**
 * Bean used to hold form messages per field. Stored under <code>messagesPerField</code> key in Freemarker context.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class MessagesPerFieldBean {

    private Map<String, MessageBean> messagesPerField = new HashMap<String, MessageBean>();

    public void addMessage(String field, String messageText, MessageType messageType) {
        if (messageText == null || messageText.trim().isEmpty())
            return;
        if (field == null)
            field = "global";

        MessageBean fm = messagesPerField.get(field);
        if (fm == null) {
            messagesPerField.put(field, new MessageBean(messageText, messageType));
        } else {
            fm.appendSummaryLine(messageText);
        }
    }

    /**
     * Check if message for given field exists
     *
     * @param field
     * @return
     */
    public boolean exists(String field) {
        return messagesPerField.containsKey(field);
    }

    /**
     * Check if exists error message for given fields
     *
     * @param fields
     * @return
     */
    public boolean existsError(String... fields) {
        for (String field : fields) {
            if (exists(field) && messagesPerField.get(field).isError())
                return true;
        }
        return false;
    }

    /**
     * Get first error message for given fields
     *
     * @param fields
     * @return message text or empty string
     */
    public String getFirstError(String... fields) {
        for (String field : fields) {
            if (existsError(field)) {
                return get(field);
            }
        }
        return "";
    }

    /**
     * Get message for given field.
     *
     * @param fieldName
     * @return message text or empty string
     */
    public String get(String fieldName) {
        MessageBean mb = messagesPerField.get(fieldName);
        if (mb != null) {
            return mb.getSummary();
        } else {
            return "";
        }
    }

    /**
     * Print text if message for given field exists. Useful eg. to add css styles for fields with message.
     *
     * @param fieldName to check for
     * @param text to print
     * @return text if message exists for given field, else empty string
     */
    public String printIfExists(String fieldName, String text) {
        if (exists(fieldName))
            return text;
        else
            return "";
    }

}
