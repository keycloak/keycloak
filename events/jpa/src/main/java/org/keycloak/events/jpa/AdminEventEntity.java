package org.keycloak.events.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
@Entity
@Table(name="ADMIN_EVENT_ENTITY")
public class AdminEventEntity {
    
    @Id
    @Column(name="ID", length = 36)
    private String id;
    
    @Column(name="ADMIN_EVENT_TIME")
    private long time;
    
    @Column(name="OPERATION_TYPE")
    private String operationType;
    
    @Column(name="REALM_ID")
    private String authRealmId;
    
    @Column(name="CLIENT_ID")
    private String authClientId;

    @Column(name="USER_ID")
    private String authUserId;
    
    @Column(name="IP_ADDRESS")
    private String authIpAddress;
    
    @Column(name="RESOURCE_PATH")
    private String resourcePath;

    @Column(name="REPRESENTATION", length = 25500)
    private String representation;

    @Column(name="ERROR")
    private String error;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getAuthRealmId() {
        return authRealmId;
    }

    public void setAuthRealmId(String authRealmId) {
        this.authRealmId = authRealmId;
    }

    public String getAuthClientId() {
        return authClientId;
    }

    public void setAuthClientId(String authClientId) {
        this.authClientId = authClientId;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public String getAuthIpAddress() {
        return authIpAddress;
    }

    public void setAuthIpAddress(String authIpAddress) {
        this.authIpAddress = authIpAddress;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getRepresentation() {
        return representation;
    }

    public void setRepresentation(String representation) {
        this.representation = representation;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
