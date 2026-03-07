package org.keycloak.protocol.ssf.event;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.protocol.ssf.event.types.GenericSsfEvent;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.caep.AssuranceLevelChange;
import org.keycloak.protocol.ssf.event.types.caep.CaepEvent;
import org.keycloak.protocol.ssf.event.types.caep.CredentialChange;
import org.keycloak.protocol.ssf.event.types.caep.DeviceComplianceChange;
import org.keycloak.protocol.ssf.event.types.caep.SessionEstablished;
import org.keycloak.protocol.ssf.event.types.caep.SessionPresented;
import org.keycloak.protocol.ssf.event.types.caep.SessionRevoked;
import org.keycloak.protocol.ssf.event.types.caep.TokenClaimsChanged;
import org.keycloak.protocol.ssf.event.types.risc.AccountCredentialChangeRequired;
import org.keycloak.protocol.ssf.event.types.risc.AccountDisabled;
import org.keycloak.protocol.ssf.event.types.risc.AccountEnabled;
import org.keycloak.protocol.ssf.event.types.risc.AccountPurged;
import org.keycloak.protocol.ssf.event.types.risc.CredentialCompromise;
import org.keycloak.protocol.ssf.event.types.risc.IdentifierChanged;
import org.keycloak.protocol.ssf.event.types.risc.IdentifierRecycled;
import org.keycloak.protocol.ssf.event.types.risc.OptIn;
import org.keycloak.protocol.ssf.event.types.risc.OptOutCancelled;
import org.keycloak.protocol.ssf.event.types.risc.OptOutEffective;
import org.keycloak.protocol.ssf.event.types.risc.OptOutInitiated;
import org.keycloak.protocol.ssf.event.types.risc.RecoveryActivated;
import org.keycloak.protocol.ssf.event.types.risc.RecoveryInformationChanged;
import org.keycloak.protocol.ssf.event.types.risc.RiscEvent;
import org.keycloak.protocol.ssf.event.types.stream.StreamEvent;
import org.keycloak.protocol.ssf.event.types.stream.StreamUpdatedEvent;
import org.keycloak.protocol.ssf.event.types.stream.VerificationEvent;

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
                new VerificationEvent(), //
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
