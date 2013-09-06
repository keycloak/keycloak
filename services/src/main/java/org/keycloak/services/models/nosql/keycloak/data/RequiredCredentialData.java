package org.keycloak.services.models.nosql.keycloak.data;

import org.keycloak.services.models.nosql.api.AbstractNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "requiredCredentials")
public class RequiredCredentialData extends AbstractNoSQLObject {

    public static final int CLIENT_TYPE_USER = 1;
    public static final int CLIENT_TYPE_RESOURCE = 2;
    public static final int CLIENT_TYPE_OAUTH_RESOURCE = 3;

    private String id;

    private String type;
    private boolean input;
    private boolean secret;
    private String formLabel;

    private String realmId;
    private int clientType;

    @NoSQLId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NoSQLField
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @NoSQLField
    public boolean isInput() {
        return input;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    @NoSQLField
    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    @NoSQLField
    public String getFormLabel() {
        return formLabel;
    }

    public void setFormLabel(String formLabel) {
        this.formLabel = formLabel;
    }

    @NoSQLField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @NoSQLField
    public int getClientType() {
        return clientType;
    }

    public void setClientType(int clientType) {
        this.clientType = clientType;
    }
}
