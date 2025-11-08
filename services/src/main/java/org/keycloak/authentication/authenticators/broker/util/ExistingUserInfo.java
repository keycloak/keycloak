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

package org.keycloak.authentication.authenticators.broker.util;

import java.io.IOException;

import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExistingUserInfo {
    private String existingUserId;
    private String duplicateAttributeName;
    private String duplicateAttributeValue;

    public ExistingUserInfo() {}

    public ExistingUserInfo(String existingUserId, String duplicateAttributeName, String duplicateAttributeValue) {
        this.existingUserId = existingUserId;
        this.duplicateAttributeName = duplicateAttributeName;
        this.duplicateAttributeValue = duplicateAttributeValue;
    }

    public String getExistingUserId() {
        return existingUserId;
    }

    public void setExistingUserId(String existingUserId) {
        this.existingUserId = existingUserId;
    }

    public String getDuplicateAttributeName() {
        return duplicateAttributeName;
    }

    public void setDuplicateAttributeName(String duplicateAttributeName) {
        this.duplicateAttributeName = duplicateAttributeName;
    }

    public String getDuplicateAttributeValue() {
        return duplicateAttributeValue;
    }

    public void setDuplicateAttributeValue(String duplicateAttributeValue) {
        this.duplicateAttributeValue = duplicateAttributeValue;
    }

    public String serialize() {
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ExistingUserInfo deserialize(String serialized) {
        try {
            return JsonSerialization.readValue(serialized, ExistingUserInfo.class);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
