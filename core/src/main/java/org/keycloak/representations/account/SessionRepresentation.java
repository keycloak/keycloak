package org.keycloak.representations.account;

import java.util.List;

/**
 * Created by st on 29/03/17.
 */
public class SessionRepresentation {

    private String id;
    private String ipAddress;
    private int started;
    private int lastAccess;
    private int expires;
    private List<ClientRepresentation> clients;
    private String browser;
    private Boolean current;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.started = started;
    }

    public int getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(int lastAccess) {
        this.lastAccess = lastAccess;
    }

    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

    public List<ClientRepresentation> getClients() {
        return clients;
    }

    public void setClients(List<ClientRepresentation> clients) {
        this.clients = clients;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getBrowser() {
        return browser;
    }

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }
}
