package org.keycloak.protocol.ssf.event.types.caep;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * The Credential Change event signals that a credential was created, changed, revoked or deleted.
 */
public class CredentialChange extends CaepEvent {

    /**
     * See: https://openid.github.io/sharedsignals/openid-caep-1_0.html#name-credential-change
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/caep/event-type/credential-change";


    /**
     * This MUST be one of the following strings, or any other credential type supported mutually by the Transmitter and the Receiver.
     * password
     * pin
     * x509
     * fido2-platform
     * fido2-roaming
     * fido-u2f
     * verifiable-credential
     * phone-voice
     * phone-sms
     * app
     */
    @JsonProperty("credential_type")
    protected CredentialType credentialType;

    /**
     * This MUST be one of the following strings:
     *
     * create
     * revoke
     * update
     * delete
     */
    @JsonProperty("change_type")
    @JsonDeserialize(using = ChangeTypeDeserializer.class)
    protected ChangeType changeType;

    /**
     * credential friendly name
     */
    @JsonProperty("friendly_name")
    protected String friendlyName;

    /**
     * issuer of the X.509 certificate as defined in [RFC5280]
     */
    @JsonProperty("x509_issuer")
    protected String x509Issuer;

    /**
     * serial number of the X.509 certificate as defined in [RFC5280]
     */
    @JsonProperty("x509_serial")
    protected String x509Serial;

    /**
     * FIDO2 Authenticator Attestation GUID as defined in [WebAuthn]
     */
    @JsonProperty("fido2_aaguid")
    protected String fido2Aaguid;

    public CredentialChange() {
        super(TYPE);
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getX509Issuer() {
        return x509Issuer;
    }

    public void setX509Issuer(String x509Issuer) {
        this.x509Issuer = x509Issuer;
    }

    public String getX509Serial() {
        return x509Serial;
    }

    public void setX509Serial(String x509Serial) {
        this.x509Serial = x509Serial;
    }

    public String getFido2Aaguid() {
        return fido2Aaguid;
    }

    public void setFido2Aaguid(String fido2Aaguid) {
        this.fido2Aaguid = fido2Aaguid;
    }

    /**
     * See: https://openid.net/specs/openid-caep-specification-1_0.html#rfc.section.3.3.1
     */
    public enum CredentialType {

        PASSWORD("password"),
        PIN("pin"),
        X509("x509"),
        FIDO2_PLATFORM("fido2-platform"),
        FIDO2_ROAMING("fido2-roaming"),
        FIDO2_U2F("fido-u2f"),
        VERIFIABLE_CREDENTIAL("verifiable-credential"),
        PHONE_VOICE("phone-voice"),
        PHONE_SMS("phone-sms"),
        APP("app");

        private final String type;

        CredentialType(String type) {
            this.type = type;
        }

        @JsonValue
        public String getType() {
            return type;
        }
    }

    /**
     * See: https://openid.net/specs/openid-caep-specification-1_0.html#rfc.section.3.3.1
     */
    public enum ChangeType {

        CREATE("create"),
        REVOKE("revoke"),
        UPDATE("update"),
        DELETE("delete");

        private final String type;

        ChangeType(String type) {
            this.type = type;
        }

        @JsonValue
        public String getType() {
            return type;
        }
    }

    @Override
    public String toString() {
        return "CredentialChange{" +
               "credentialType=" + credentialType +
               ", changeType=" + changeType +
               ", friendlyName='" + friendlyName + '\'' +
               ", x509Issuer='" + x509Issuer + '\'' +
               ", x509Serial='" + x509Serial + '\'' +
               ", fido2Aaguid='" + fido2Aaguid + '\'' +
               '}';
    }
}
