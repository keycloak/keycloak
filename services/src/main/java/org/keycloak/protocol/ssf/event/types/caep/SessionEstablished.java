package org.keycloak.protocol.ssf.event.types.caep;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * The Session Established event signifies that the Transmitter has established a new session for the subject.
 * Receivers may use this information for a number of reasons, including:
 * <ul>
 * <li>A service acting as a Transmitter can close the loop with the IdP after a user has been federated from the IdP</li>
 * <li>An IdP can detect unintended logins</li>
 * <li>A Receiver can establish an inventory of user sessions</li>
 * </ul>
 * The event_timestamp in this event type specifies the time at which the session was established.
 */
public class SessionEstablished extends CaepEvent {

    /**
     * See: https://openid.github.io/sharedsignals/openid-caep-1_0.html#name-session-established
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/caep/event-type/session-established";

    /**
     * The array of IP addresses of the user as observed by the Transmitter. The value MUST be in the format of an array of strings, each one of which represents the RFC 4001 [RFC4001] string representation of an IP address. (NOTE, this can be different from the one observed by the Receiver for the same user because of network translation).
     */
    @JsonProperty("ips")
    protected Set<String> ips;

    /**
     * Fingerprint of the user agent computed by the Transmitter. (NOTE, this is not to identify the session, but to present some qualities of the session)
     */
    @JsonProperty("fp_ua")
    protected String fingerPrintUserAgent;

    /**
     * The authentication context class reference of the session, as established by the Transmitter. The value of this field MUST be interpreted in the same way as the corresponding field in an OpenID Connect ID Token [OpenID.Core]
     */
    @JsonProperty("acr")
    protected String acr;

    /**
     * The authentication methods reference of the session, as established by the Transmitter. The value of this field MUST be an array of strings, each of which MUST be interpreted in the same way as the corresponding field in an OpenID Connect ID Token [OpenID.Core]
     */
    @JsonProperty("amr")
    protected String amr;

    /**
     * The external session identifier, which may be used to correlate this session with a broader session (e.g., a federated session established using SAML)
     */
    @JsonProperty("ext_id")
    protected String extId;

    public SessionEstablished() {
        super(TYPE);
    }

    public Set<String> getIps() {
        return ips;
    }

    public void setIps(Set<String> ips) {
        this.ips = ips;
    }

    public String getFingerPrintUserAgent() {
        return fingerPrintUserAgent;
    }

    public void setFingerPrintUserAgent(String fingerPrintUserAgent) {
        this.fingerPrintUserAgent = fingerPrintUserAgent;
    }

    public String getAcr() {
        return acr;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public String getAmr() {
        return amr;
    }

    public void setAmr(String amr) {
        this.amr = amr;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    @Override
    public String toString() {
        return "SessionEstablished{" +
               "ips=" + ips +
               ", fingerPrintUserAgent='" + fingerPrintUserAgent + '\'' +
               ", acr='" + acr + '\'' +
               ", amr='" + amr + '\'' +
               ", extId='" + extId + '\'' +
               '}';
    }
}
