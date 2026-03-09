package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://openid.net/specs/openid-sse-framework-1_0.html#complex-subjects
 */
public class ComplexSubjectId extends SubjectId {

    public static final String TYPE = "complex";

    /**
     * The user involved with the event
     */
    @JsonProperty("user")
    protected SubjectId user;

    /**
     * The device involved with the event
     */
    @JsonProperty("device")
    protected SubjectId device;

    /**
     * The session involved with the event
     */
    @JsonProperty("session")
    protected SubjectId session;

    /**
     * The application involved with the event
     */
    @JsonProperty("application")
    protected SubjectId application;

    /**
     * The tenant involved with the event
     */
    @JsonProperty("tenant")
    protected SubjectId tenant;

    /**
     * The org_unit involved with the event
     */
    @JsonProperty("org_unit")
    protected SubjectId orgUnit;

    /**
     * The group involved with the event
     */
    @JsonProperty("group")
    protected SubjectId group;

    public ComplexSubjectId() {
        super(TYPE);
    }

    public SubjectId getUser() {
        return user;
    }

    public void setUser(SubjectId user) {
        this.user = user;
    }

    public SubjectId getDevice() {
        return device;
    }

    public void setDevice(SubjectId device) {
        this.device = device;
    }

    public SubjectId getSession() {
        return session;
    }

    public void setSession(SubjectId session) {
        this.session = session;
    }

    public SubjectId getApplication() {
        return application;
    }

    public void setApplication(SubjectId application) {
        this.application = application;
    }

    public SubjectId getTenant() {
        return tenant;
    }

    public void setTenant(SubjectId tenant) {
        this.tenant = tenant;
    }

    public SubjectId getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(SubjectId orgUnit) {
        this.orgUnit = orgUnit;
    }

    public SubjectId getGroup() {
        return group;
    }

    public void setGroup(SubjectId group) {
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
