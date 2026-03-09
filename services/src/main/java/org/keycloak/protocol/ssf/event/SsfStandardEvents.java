package org.keycloak.protocol.ssf.event;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.protocol.ssf.event.caep.AssuranceLevelChange;
import org.keycloak.protocol.ssf.event.caep.CaepEvent;
import org.keycloak.protocol.ssf.event.caep.CredentialChange;
import org.keycloak.protocol.ssf.event.caep.DeviceComplianceChange;
import org.keycloak.protocol.ssf.event.caep.SessionEstablished;
import org.keycloak.protocol.ssf.event.caep.SessionPresented;
import org.keycloak.protocol.ssf.event.caep.SessionRevoked;
import org.keycloak.protocol.ssf.event.caep.TokenClaimsChanged;
import org.keycloak.protocol.ssf.event.risc.AccountCredentialChangeRequired;
import org.keycloak.protocol.ssf.event.risc.AccountDisabled;
import org.keycloak.protocol.ssf.event.risc.AccountEnabled;
import org.keycloak.protocol.ssf.event.risc.AccountPurged;
import org.keycloak.protocol.ssf.event.risc.CredentialCompromise;
import org.keycloak.protocol.ssf.event.risc.IdentifierChanged;
import org.keycloak.protocol.ssf.event.risc.IdentifierRecycled;
import org.keycloak.protocol.ssf.event.risc.OptIn;
import org.keycloak.protocol.ssf.event.risc.OptOutCancelled;
import org.keycloak.protocol.ssf.event.risc.OptOutEffective;
import org.keycloak.protocol.ssf.event.risc.OptOutInitiated;
import org.keycloak.protocol.ssf.event.risc.RecoveryActivated;
import org.keycloak.protocol.ssf.event.risc.RecoveryInformationChanged;
import org.keycloak.protocol.ssf.event.risc.RiscEvent;
import org.keycloak.protocol.ssf.event.stream.StreamEvent;
import org.keycloak.protocol.ssf.event.stream.StreamUpdatedEvent;
import org.keycloak.protocol.ssf.event.stream.StreamVerificationEvent;

/**
 * Registry of Standard SSF Events.
 */
public class SsfStandardEvents {

    /**
     * Holds all standard SSF Stream events.
     */
    public static final Map<String, Class<? extends StreamEvent>> STREAM_EVENT_TYPES;

    /**
     * Holds all standard CAEP events.
     */
    public static final Map<String, Class<? extends CaepEvent>> CAEP_EVENT_TYPES;

    /**
     * Holds all standard RISC events.
     */
    public static final Map<String, Class<? extends RiscEvent>> RISC_EVENT_TYPES;

    static {
        var ssfStreamEventTypes = new HashMap<String, Class<? extends StreamEvent>>();
        List.of(//
                new StreamVerificationEvent(), //
                new StreamUpdatedEvent() //
        ).forEach(ssfEvent -> ssfStreamEventTypes.put(ssfEvent.getEventType(), ssfEvent.getClass()));
        STREAM_EVENT_TYPES = Collections.unmodifiableMap(ssfStreamEventTypes);

        var caepEventTypes = new HashMap<String, Class<? extends CaepEvent>>();
        List.of( //
                new AssuranceLevelChange(), //
                new CredentialChange(), //
                new DeviceComplianceChange(), //
                new SessionEstablished(), //
                new SessionPresented(), //
                new SessionRevoked(), //
                new TokenClaimsChanged() //
        ).forEach(caepEvent -> caepEventTypes.put(caepEvent.getEventType(), caepEvent.getClass()));
        CAEP_EVENT_TYPES = Collections.unmodifiableMap(caepEventTypes);

        var riscEventTypes = new HashMap<String, Class<? extends RiscEvent>>();
        List.of( //
                new AccountCredentialChangeRequired(), //
                new AccountDisabled(), //
                new AccountEnabled(), //
                new AccountPurged(), //
                new CredentialCompromise(), //
                new IdentifierChanged(), //
                new IdentifierRecycled(), //
                new OptIn(), //
                new OptOutInitiated(), //
                new OptOutCancelled(), //
                new OptOutEffective(), //
                new RecoveryActivated(), //
                new RecoveryInformationChanged() //
        ).forEach(riscEvent -> riscEventTypes.put(riscEvent.getEventType(), riscEvent.getClass()));
        RISC_EVENT_TYPES = Collections.unmodifiableMap(riscEventTypes);
    }

    public static Class<? extends SsfEvent> getSecurityEventType(String eventType) {

        var streamEventTypes = STREAM_EVENT_TYPES.get(eventType);
        if (streamEventTypes != null) {
            return streamEventTypes;
        }

        var caepEventType = CAEP_EVENT_TYPES.get(eventType);
        if (caepEventType != null) {
            return caepEventType;
        }

        var riscEventType = RISC_EVENT_TYPES.get(eventType);
        if (riscEventType != null) {
            return riscEventType;
        }

        return GenericSsfEvent.class;
    }
}
