package org.keycloak.ssf.subject;

import org.keycloak.ssf.event.caep.CaepSessionRevoked;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * See: https://openid.net/specs/openid-sse-framework-1_0.html#complex-subjects
 *
 * <p>Every nested field is typed as the abstract {@link SubjectId} and
 * therefore annotated with {@link JsonDeserialize} using
 * {@link SubjectIdJsonDeserializer}, which dispatches on the {@code format}
 * discriminator. Without this, Jackson's default bean deserialization tries
 * to instantiate the abstract class and fails — a path hit as soon as a
 * receiver parses a real transmitter-emitted SET that carries a complex
 * subject (e.g. {@code ComplexSubjectId{user: IssuerSubjectId, session:
 * OpaqueSubjectId}} on a {@link CaepSessionRevoked}
 * event).
 */
public class ComplexSubjectId extends SubjectId {

    public static final String TYPE = "complex";

    /**
     * The user involved with the event
     */
    @JsonProperty("user")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId user;

    /**
     * The device involved with the event
     */
    @JsonProperty("device")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId device;

    /**
     * The session involved with the event
     */
    @JsonProperty("session")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId session;

    /**
     * The application involved with the event
     */
    @JsonProperty("application")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId application;

    /**
     * The tenant involved with the event
     */
    @JsonProperty("tenant")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId tenant;

    /**
     * The org_unit involved with the event
     */
    @JsonProperty("org_unit")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId orgUnit;

    /**
     * The group involved with the event
     */
    @JsonProperty("group")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
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
