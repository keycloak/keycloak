package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * See: https://openid.net/specs/openid-sse-framework-1_0.html#complex-subjects
 */
public class ComplexSubjectId extends SubjectId {

    public static final String TYPE = "complex";

    /**
     * The user involved with the event
     */
    @JsonProperty("user")
    protected Map<String, String> user;

    /**
     * The device involved with the event
     */
    @JsonProperty("device")
    protected Map<String, String> device;

    /**
     * The session involved with the event
     */
    @JsonProperty("session")
    protected Map<String, String> session;

    /**
     * The application involved with the event
     */
    @JsonProperty("application")
    protected Map<String, String> application;

    /**
     * The tenant involved with the event
     */
    @JsonProperty("tenant")
    protected Map<String, String> tenant;

    /**
     * The org_unit involved with the event
     */
    @JsonProperty("org_unit")
    protected Map<String, String> orgUnit;

    /**
     * The group involved with the event
     */
    @JsonProperty("group")
    protected Map<String, String> group;

    public ComplexSubjectId() {
        super(TYPE);
    }

    public Map<String, String> getUser() {
        return user;
    }

    public void setUser(Map<String, String> user) {
        this.user = user;
    }

    public Map<String, String> getDevice() {
        return device;
    }

    public void setDevice(Map<String, String> device) {
        this.device = device;
    }

    public Map<String, String> getSession() {
        return session;
    }

    public void setSession(Map<String, String> session) {
        this.session = session;
    }

    public Map<String, String> getApplication() {
        return application;
    }

    public void setApplication(Map<String, String> application) {
        this.application = application;
    }

    public Map<String, String> getTenant() {
        return tenant;
    }

    public void setTenant(Map<String, String> tenant) {
        this.tenant = tenant;
    }

    public Map<String, String> getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(Map<String, String> orgUnit) {
        this.orgUnit = orgUnit;
    }

    public Map<String, String> getGroup() {
        return group;
    }

    public void setGroup(Map<String, String> group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "ComplexSubjectId{" +
               "user=" + user +
               ", device=" + device +
               ", session=" + session +
               ", application=" + application +
               ", tenant=" + tenant +
               ", orgUnit=" + orgUnit +
               ", group=" + group +
               '}';
    }
}
