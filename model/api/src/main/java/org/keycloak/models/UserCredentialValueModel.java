package org.keycloak.models;

/**
 * Used just in cases when we want to "directly" update or retrieve the hash or salt of user credential (For example during export/import)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserCredentialValueModel {

    private String type;
    private String value;
    private String device;
    private byte[] salt;
    private int hashIterations;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public void setHashIterations(int iterations) {
        this.hashIterations = iterations;
    }
}
