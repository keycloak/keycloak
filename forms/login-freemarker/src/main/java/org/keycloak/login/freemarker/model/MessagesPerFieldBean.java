/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.login.freemarker.model;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.login.FormMessage;
import org.keycloak.login.freemarker.FreeMarkerLoginFormsProvider.MessageType;

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
            field = FormMessage.GLOBAL;

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
