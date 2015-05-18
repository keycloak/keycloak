package org.keycloak.events.admin;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdminEvent {

    private long time;
    
    private String realmId;

    private AuthDetails authDetails;

    private OperationType operationType;

    private String resourcePath;

    private String representation;

    private String error;
    
    /**
     * Returns the time of the event
     *
     * @return time in millis
     */
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
    
    /**
     * Returns the id of the realm
     *
     * @return
     */
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    /**
     * Returns authentication details
     *
     * @return
     */
    public AuthDetails getAuthDetails() {
        return authDetails;
    }

    public void setAuthDetails(AuthDetails authDetails) {
        this.authDetails = authDetails;
    }

    /**
     * Returns the type of the operation
     *
     * @return
     */
    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    /**
     * Returns the path of the resource. For example:
     * <ul>
     *     <li><b>realms</b> - realm list</li>
     *     <li><b>realms/master</b> - master realm</li>
     *     <li><b>realms/clients/00d4b16f-f1f9-4e73-8366-d76b18f3e0e1</b> - client within the master realm</li>
     * </ul>
     *
     * @return
     */
    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * Returns the updated JSON representation if <code>operationType</code> is <code>CREATE</code> or <code>UPDATE</code>.
     * Otherwise returns <code>null</code>.
     *
     * @return
     */
    public String getRepresentation() {
        return representation;
    }

    public void setRepresentation(String representation) {
        this.representation = representation;
    }

    /**
     * If the event was unsuccessful returns the error message. Otherwise returns <code>null</code>.
     *
     * @return
     */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
