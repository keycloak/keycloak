package org.keycloak.scripting;

import org.keycloak.models.ScriptModel;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class Script implements ScriptModel {

    private String id;

    private String realmId;

    private String name;

    private String mimeType;

    private String code;

    private String description;

    public Script(String id, String realmId, String name, String mimeType, String code, String description) {

        this.id = id;
        this.realmId = realmId;
        this.name = name;
        this.mimeType = mimeType;
        this.code = code;
        this.description = description;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Script{" +
                "id='" + id + '\'' +
                ", realmId='" + realmId + '\'' +
                ", name='" + name + '\'' +
                ", type='" + mimeType + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
