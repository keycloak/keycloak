package org.keycloak.protocol.ssf.event;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.protocol.ssf.event.caep.CaepAssuranceLevelChange;
import org.keycloak.protocol.ssf.event.caep.CaepCredentialChange;
import org.keycloak.protocol.ssf.event.caep.CaepDeviceComplianceChange;
import org.keycloak.protocol.ssf.event.caep.CaepEvent;
import org.keycloak.protocol.ssf.event.caep.CaepSessionEstablished;
import org.keycloak.protocol.ssf.event.caep.CaepSessionPresented;
import org.keycloak.protocol.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.protocol.ssf.event.caep.CaepTokenClaimsChanged;
import org.keycloak.protocol.ssf.event.risc.RiscAccountCredentialChangeRequired;
import org.keycloak.protocol.ssf.event.risc.RiscAccountDisabled;
import org.keycloak.protocol.ssf.event.risc.RiscAccountEnabled;
import org.keycloak.protocol.ssf.event.risc.RiscAccountPurged;
import org.keycloak.protocol.ssf.event.risc.RiscCredentialCompromise;
import org.keycloak.protocol.ssf.event.risc.RiscEvent;
import org.keycloak.protocol.ssf.event.risc.RiscIdentifierChanged;
import org.keycloak.protocol.ssf.event.risc.RiscIdentifierRecycled;
import org.keycloak.protocol.ssf.event.risc.RiscOptIn;
import org.keycloak.protocol.ssf.event.risc.RiscOptOutCancelled;
import org.keycloak.protocol.ssf.event.risc.RiscOptOutEffective;
import org.keycloak.protocol.ssf.event.risc.RiscOptOutInitiated;
import org.keycloak.protocol.ssf.event.risc.RiscRecoveryActivated;
import org.keycloak.protocol.ssf.event.risc.RiscRecoveryInformationChanged;
import org.keycloak.protocol.ssf.event.stream.SsfStreamEvent;
import org.keycloak.protocol.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.protocol.ssf.event.stream.SsfStreamVerificationEvent;

/**
 * Registry of Standard SSF Events.
 */
public class SsfStandardEvents {

    /**
     * Holds all standard SSF Stream events.
     */
    public static final Map<String, Class<? extends SsfStreamEvent>> STREAM_EVENT_TYPES;

    /**
     * Holds all standard CAEP events.
     */
    public static final Map<String, Class<? extends CaepEvent>> CAEP_EVENT_TYPES;

    /**
     * Holds all standard RISC events.
     */
    public static final Map<String, Class<? extends RiscEvent>> RISC_EVENT_TYPES;

    public static final Map<String, Class<? extends SsfEvent>> SIMPLE_EVENT_TYPES_MAP;

    static {

        var simpleEventTypesMap = new HashMap<String, Class<? extends SsfEvent>>();

        var ssfStreamEventTypes = new HashMap<String, Class<? extends SsfStreamEvent>>();
        List.of(//
                new SsfStreamVerificationEvent(), //
                new SsfStreamUpdatedEvent() //
        ).forEach(ssfEvent -> {
            simpleEventTypesMap.put(ssfEvent.getClass().getSimpleName(), ssfEvent.getClass());
            ssfStreamEventTypes.put(ssfEvent.getEventType(), ssfEvent.getClass());
        });
        STREAM_EVENT_TYPES = Collections.unmodifiableMap(ssfStreamEventTypes);

        var caepEventTypes = new HashMap<String, Class<? extends CaepEvent>>();
        List.of( //
                new CaepAssuranceLevelChange(), //
                new CaepCredentialChange(), //
                new CaepDeviceComplianceChange(), //
                new CaepSessionEstablished(), //
                new CaepSessionPresented(), //
                new CaepSessionRevoked(), //
                new CaepTokenClaimsChanged() //
        ).forEach(caepEvent -> {
            simpleEventTypesMap.put(caepEvent.getClass().getSimpleName(), caepEvent.getClass());
            caepEventTypes.put(caepEvent.getEventType(), caepEvent.getClass());
        });
        CAEP_EVENT_TYPES = Collections.unmodifiableMap(caepEventTypes);

        var riscEventTypes = new HashMap<String, Class<? extends RiscEvent>>();
        List.of( //
                new RiscAccountCredentialChangeRequired(), //
                new RiscAccountDisabled(), //
                new RiscAccountEnabled(), //
                new RiscAccountPurged(), //
                new RiscCredentialCompromise(), //
                new RiscIdentifierChanged(), //
                new RiscIdentifierRecycled(), //
                new RiscOptIn(), //
                new RiscOptOutInitiated(), //
                new RiscOptOutCancelled(), //
                new RiscOptOutEffective(), //
                new RiscRecoveryActivated(), //
                new RiscRecoveryInformationChanged() //
        ).forEach(riscEvent -> {
            simpleEventTypesMap.put(riscEvent.getClass().getSimpleName(), riscEvent.getClass());
            riscEventTypes.put(riscEvent.getEventType(), riscEvent.getClass());
        });
        RISC_EVENT_TYPES = Collections.unmodifiableMap(riscEventTypes);

        SIMPLE_EVENT_TYPES_MAP = Collections.unmodifiableMap(simpleEventTypesMap);
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

    /**
     * Resolves the security event type based on the given simple event type string.
     *
     * @param eventType the string representation of the event type to be resolved
     * @return the class of the corresponding {@link SsfEvent}, or null if no matching type is found
     */
    public static Class<? extends SsfEvent> resolveSecurityEventType(String eventType) {
        return SIMPLE_EVENT_TYPES_MAP.get(eventType);
    }
}
