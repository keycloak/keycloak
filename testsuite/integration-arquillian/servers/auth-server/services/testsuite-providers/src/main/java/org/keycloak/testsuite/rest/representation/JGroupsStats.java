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

package org.keycloak.testsuite.rest.representation;

import java.text.NumberFormat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JGroupsStats {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    static {
        NUMBER_FORMAT.setGroupingUsed(true);
    }

    private long sentBytes;
    private long sentMessages;
    private long receivedBytes;
    private long receivedMessages;

    public JGroupsStats() {
    }

    public JGroupsStats(long sentBytes, long sentMessages, long receivedBytes, long receivedMessages) {
        this.sentBytes = sentBytes;
        this.sentMessages = sentMessages;
        this.receivedBytes = receivedBytes;
        this.receivedMessages = receivedMessages;
    }

    public long getSentBytes() {
        return sentBytes;
    }

    public void setSentBytes(long sentBytes) {
        this.sentBytes = sentBytes;
    }

    public long getSentMessages() {
        return sentMessages;
    }

    public void setSentMessages(long sentMessages) {
        this.sentMessages = sentMessages;
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    public void setReceivedBytes(long receivedBytes) {
        this.receivedBytes = receivedBytes;
    }

    public long getReceivedMessages() {
        return receivedMessages;
    }

    public void setReceivedMessages(long receivedMessages) {
        this.receivedMessages = receivedMessages;
    }

    public String statsAsString() {
        return String.format("sentBytes: %s, sentMessages: %d, receivedBytes: %s, receivedMessages: %d",
                NUMBER_FORMAT.format(sentBytes), sentMessages, NUMBER_FORMAT.format(receivedBytes), receivedMessages);
    }
}
