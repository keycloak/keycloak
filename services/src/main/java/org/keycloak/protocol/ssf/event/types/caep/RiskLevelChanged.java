package org.keycloak.protocol.ssf.event.types.caep;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A vendor may deploy mechanisms to gather and analyze various signals associated with subjects such as users, devices, etc. These signals, which can originate from diverse channels and methods beyond the scope of this event description, are processed to derive an abstracted risk level representing the subject's current threat status.
 *
 * The Risk Level Change event is employed by the Transmitter to communicate any modifications in a subject's assessed risk level at the time indicated by the event_timestamp field in the Risk Level Change event. The Transmitter may generate this event to indicate:
 */
public class RiskLevelChanged extends CaepEvent {

    /**
     * See: https://openid.github.io/sharedsignals/openid-caep-1_0.html#section-3.8
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/caep/event-type/risk-level-change";

    /**
     * Indicates the reason that contributed to the risk level changes by the Transmitter.
     */
    @JsonProperty("risk_reason")
    protected String riskReason;

    /**
     * Representing the principal entity involved in the observed risk event, as identified by the transmitter. The subject principal can be one of the following entities USER, DEVICE, SESSION, TENANT, ORG_UNIT, GROUP, or any other entity as defined in Section 2 of [SSF]. This claim identifies the primary subject associated with the event, and helps to contextualize the risk relative to the entity involved.
     */
    @JsonProperty("principal")
    protected String principal;

    /**
     * Indicates the current level of the risk for the subject. Value MUST be one of LOW, MEDIUM, HIGH
     */
    @JsonProperty("current_level")
    protected String currentLevel;

    /**
     * Indicates the previously known level of the risk for the subject. Value MUST be one of LOW, MEDIUM, HIGH. If the Transmitter omits this value, the Receiver MUST assume that the previous risk level is unknown to the Transmitter.
     */
    @JsonProperty("previous_level")
    protected String previousLevel;

    public RiskLevelChanged() {
        super(TYPE);
    }

    public String getRiskReason() {
        return riskReason;
    }

    public void setRiskReason(String riskReason) {
        this.riskReason = riskReason;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getPreviousLevel() {
        return previousLevel;
    }

    public void setPreviousLevel(String previousLevel) {
        this.previousLevel = previousLevel;
    }

    @Override
    public String toString() {
        return "RiskLevelChanged{" +
               "riskReason='" + riskReason + '\'' +
               ", principal='" + principal + '\'' +
               ", currentLevel='" + currentLevel + '\'' +
               ", previousLevel='" + previousLevel + '\'' +
               '}';
    }
}
