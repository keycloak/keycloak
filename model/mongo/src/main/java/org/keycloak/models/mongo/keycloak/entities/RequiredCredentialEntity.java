package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.AbstractMongoEntity;
import org.keycloak.models.mongo.api.MongoField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RequiredCredentialEntity extends AbstractMongoEntity {

    private String type;
    private boolean input;
    private boolean secret;
    private String formLabel;

    @MongoField
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @MongoField
    public boolean isInput() {
        return input;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    @MongoField
    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    @MongoField
    public String getFormLabel() {
        return formLabel;
    }

    public void setFormLabel(String formLabel) {
        this.formLabel = formLabel;
    }
}
