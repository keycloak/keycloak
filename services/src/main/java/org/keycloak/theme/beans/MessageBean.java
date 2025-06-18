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

import org.keycloak.forms.login.MessageType;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MessageBean {

    private String summary;

    private MessageType type;

    public MessageBean(String message, MessageType type) {
        this.summary = message;
        this.type = type;
    }

    public String getSummary() {
        return summary;
    }

    public void appendSummaryLine(String newLine) {
        if (newLine == null)
            return;
        if (summary == null)
            summary = newLine;
        else
            summary = summary + "<br>" + newLine;
    }

    public String getType() {
        return this.type.toString().toLowerCase();
    }

    public boolean isSuccess() {
        return MessageType.SUCCESS.equals(this.type);
    }

    public boolean isWarning() {
        return MessageType.WARNING.equals(this.type);
    }

    public boolean isError() {
        return MessageType.ERROR.equals(this.type);
    }

}