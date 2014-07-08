package org.keycloak.models.users;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Credentials {

    private byte[] salt;
    private String type;
    private String value;
    private String device;
    private int hashIterations;

    public Credentials(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Credentials(String type, String value, String device) {
        this.type = type;
        this.value = value;
        this.device = device;
    }

    public Credentials(String type, byte[] salt, String value, int hashIterations, String device) {
        this.salt = salt;
        this.type = type;
        this.value = value;
        this.hashIterations = hashIterations;
        this.device = device;
    }

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

    public void setHashIterations(int hashIterations) {
        this.hashIterations = hashIterations;
    }
}
