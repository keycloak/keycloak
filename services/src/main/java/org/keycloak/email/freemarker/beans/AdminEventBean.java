package org.keycloak.email.freemarker.beans;

import java.util.Date;

import org.keycloak.events.admin.AdminEvent;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class AdminEventBean {
    
    private AdminEvent adminEvent;

    public AdminEventBean(AdminEvent adminEvent) {
        this.adminEvent = adminEvent;
    }

    public Date getDate() {
        return new Date(adminEvent.getTime());
    }

    public String getOperationType() {
        return adminEvent.getOperationType().toString().toLowerCase();
    }

    public String getClient() {
        return adminEvent.getAuthDetails().getClientId();
    }

    public String getIpAddress() {
        return adminEvent.getAuthDetails().getIpAddress();
    }
    
    public String getResourcePath() {
        return adminEvent.getResourcePath();
    }
}
