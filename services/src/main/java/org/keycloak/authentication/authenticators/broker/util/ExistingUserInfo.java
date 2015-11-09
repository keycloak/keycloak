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
