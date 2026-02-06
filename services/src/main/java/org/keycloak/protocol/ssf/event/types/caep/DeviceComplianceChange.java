package org.keycloak.protocol.ssf.event.types.caep;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Device Compliance Change signals that a device's compliance status has changed.
 */
public class DeviceComplianceChange extends CaepEvent {

    /**
     * See: https://openid.github.io/sharedsignals/openid-caep-1_0.html#name-device-compliance-change
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/caep/event-type/device-compliance-change";

    /**
     * The compliance status prior to the change that triggered the event
     * This MUST be one of the following strings: compliant, not-compliant
     */
    @JsonProperty("previous_status")
    protected ComplianceChange previousStatus;

    /**
     * The current status that triggered the event.
     */
    @JsonProperty("current_status")
    protected ComplianceChange currentStatus;

    public DeviceComplianceChange() {
        super(TYPE);
    }

    public ComplianceChange getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ComplianceChange previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ComplianceChange getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ComplianceChange currentStatus) {
        this.currentStatus = currentStatus;
    }

    public enum ComplianceChange {

        COMPLIANT("compliant"),
        NOT_COMPLIANT("not-compliant");

        private final String type;

        ComplianceChange(String type) {
            this.type = type;
        }

        @JsonValue
        public String getType() {
            return type;
        }
    }

    @Override
    public String toString() {
        return "DeviceComplianceChange{" +
               "previousStatus=" + previousStatus +
               ", currentStatus=" + currentStatus +
               '}';
    }
}
