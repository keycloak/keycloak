package org.keycloak.protocol.ssf.event.types.caep;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * The Session Presented event signifies that the Transmitter has observed the session to be present at the Transmitter at the time indicated by the event_timestamp field in the Session Presented event.
 * Receivers may use this information for reasons that include:
 *<ul>
 * <li>Detecting abnormal user activity</li>
 * <li>Establishing an inventory of live sessions belonging to a user</li>
 * </ul>
 */
public class SessionPresented extends CaepEvent {

    /**
     * See: https://openid.github.io/sharedsignals/openid-caep-1_0.html#name-session-established
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/caep/event-type/session-presented";

    /**
     * The array of IP addresses of the user as observed by the Transmitter. The value MUST be in the format of an array of strings, each one of which represents the RFC 4001 [RFC4001] string representation of an IP address. (NOTE, this can be different from the one observed by the Receiver for the same user because of network translation).
     */
    @JsonProperty("ips")
    protected Set<String> ips;

    /**
     * Fingerprint of the user agent computed by the Transmitter. (NOTE, this is not to identify the session, but to present some qualities of the session).
     */
    @JsonProperty("fp_ua")
    protected String fingerPrintUserAgent;

    /**
     * The external session identifier, which may be used to correlate this session with a broader session (e.g., a federated session established using SAML).
     */
    @JsonProperty("ext_id")
    protected String extId;

    public SessionPresented() {
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

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    @Override
    public String toString() {
        return "SessionPresented{" +
               "ips=" + ips +
               ", fingerPrintUserAgent='" + fingerPrintUserAgent + '\'' +
               ", extId='" + extId + '\'' +
               '}';
    }
}
