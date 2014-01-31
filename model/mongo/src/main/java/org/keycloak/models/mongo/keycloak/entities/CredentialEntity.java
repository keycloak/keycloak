package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.AbstractMongoEntity;
import org.keycloak.models.mongo.api.MongoField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialEntity extends AbstractMongoEntity {

    private String type;
    private String value;
    private String device;
    private byte[] salt;

    @MongoField
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @MongoField
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @MongoField
    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @MongoField
    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }
}
